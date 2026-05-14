package _03_advanced;

import _02_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static shared.Utils.*;

/**
 * 高级 RAG 示例 —— 条件性跳过检索（Conditional Skip Retrieval）
 *
 * 核心问题：
 * 不是所有用户查询都需要检索文档。例如：
 * - 用户说 "Hi"、"你好" → 纯属寒暄，检索文档毫无意义
 * - 用户问 "今天天气怎么样" → 与知识库无关，检索只会引入噪声
 * - 用户问 "Can I cancel my reservation?" → 需要检索租车条款文档
 *
 * 无意义的检索带来的问题：
 * 1. 浪费计算资源（嵌入模型调用、向量搜索）
 * 2. 浪费 Token 成本（召回的无关片段也会送入 LLM）
 * 3. 可能引入干扰信息，导致 LLM 产生幻觉
 *
 * 解决方案 —— 检索决策器：
 * 在检索之前增加一个判断环节，决定当前查询是否需要检索。
 * 如果不需要，直接跳过检索，让 LLM 基于自身知识回答。
 *
 * 实现方式：
 * 自定义 QueryRouter，当判断不需要检索时返回空列表，
 * 这样查询就不会被路由到任何 ContentRetriever。
 *
 * 决策方式有多种：
 * - 规则判断（如按关键词、用户权限）
 * - 语义分类（用嵌入模型判断查询类别）
 * - LLM 判断（本示例采用，最灵活）
 */
public class _06_Advanced_RAG_Skip_Retrieval_Example {

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        /**
         * 启动对话，观察检索跳过效果：
         *
         * 第一轮：说 "Hi"
         *   → LLM 判断该查询与租车业务无关
         *   → QueryRouter 返回 emptyList()，跳过检索
         *   → 日志输出 "LLM decided: no"
         *   → LLM 直接基于自身知识寒暄回复
         *
         * 第二轮：问 "Can I cancel my reservation?"
         *   → LLM 判断该查询与租车业务相关
         *   → QueryRouter 返回 [contentRetriever]，执行检索
         *   → 日志输出 "LLM decided: yes"
         *   → LLM 基于检索到的条款片段回答
         */
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // ==================== 第一步：准备嵌入模型和向量存储 ====================

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        /**
         * 加载租车公司服务条款文档，构建向量存储。
         * embed() 是本地辅助方法，封装了完整的文档处理流水线。
         */
        EmbeddingStore<TextSegment> embeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);

        // ==================== 第二步：配置内容检索器 ====================

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // ==================== 第三步：创建对话模型 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        // ==================== 第四步：创建智能查询路由器（核心）====================

        /**
         * 自定义 QueryRouter，用 LLM 判断是否需要检索。
         *
         * 实现原理：
         * 1. 定义一个 PromptTemplate，询问 LLM 当前查询是否与租车业务相关
         * 2. 每次用户查询时，先用 LLM 做二分类判断（yes/no/maybe）
         * 3. 如果回答包含 "no"，返回空列表 → 跳过检索
         * 4. 否则返回包含 contentRetriever 的列表 → 执行检索
         *
         * 设计要点：
         * - 要求 LLM 只回答 'yes'/'no'/'maybe'，便于程序解析
         * - 使用 {{it}} 占位符注入用户查询
         * - 判断逻辑简单明确，减少 LLM 误判
         */
        QueryRouter queryRouter = new QueryRouter() {

            /**
             * Prompt 模板：让 LLM 判断查询是否与租车公司业务相关。
             *
             * 模板变量 {{it}} 会被替换为用户的实际查询文本。
             *
             * 指令设计：
             * - 明确限定回答格式（只准说 yes/no/maybe）
             * - 明确判断标准（与租车公司业务的关联性）
             * - 简洁直接，减少 LLM 发挥空间
             */
            private final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from(
                    "Is the following query related to the business of the car rental company? " +
                            "Answer only 'yes', 'no' or 'maybe'. " +
                            "Query: {{it}}"
            );

            @Override
            public Collection<ContentRetriever> route(Query query) {

                // 将用户查询填充到模板中，生成完整 Prompt
                Prompt prompt = PROMPT_TEMPLATE.apply(query.text());

                // 调用 LLM 做判断，获取 AI 回复
                AiMessage aiMessage = chatModel.chat(prompt.toUserMessage()).aiMessage();

                // 打印决策结果，便于调试观察
                System.out.println("LLM decided: " + aiMessage.text());

                /**
                 * 解析 LLM 回答，决定是否跳过检索。
                 *
                 * 如果 LLM 回答包含 "no"（不区分大小写）：
                 * - 返回 emptyList()，表示不路由到任何检索器
                 * - 后续 LLM 将直接基于自身知识回答，不引用文档
                 *
                 * 否则（yes 或 maybe）：
                 * - 返回 singletonList(contentRetriever)，执行正常检索流程
                 */
                if (aiMessage.text().toLowerCase().contains("no")) {
                    return emptyList();
                }

                return singletonList(contentRetriever);
            }
        };

        // ==================== 第五步：组装检索增强器 ====================

        /**
         * DefaultRetrievalAugmentor 配置自定义 QueryRouter。
         *
         * 执行流程：
         * 1. 用户查询进入 QueryRouter
         * 2. QueryRouter 内部调用 LLM 做检索决策
         * 3. 如果返回空列表 → 跳过检索，直接走 LLM 对话
         * 4. 如果返回检索器列表 → 执行检索，将片段注入 Prompt
         */
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        // ==================== 第六步：组装 AI Service ====================

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    /**
     * 辅助方法：将单个文档完整处理为向量存储。
     *
     * 封装了 RAG 预处理的标准流水线：
     * 1. 使用 TextDocumentParser 解析文档
     * 2. 使用递归分割器将文档切分为 TextSegment（300 tokens/段，无重叠）
     * 3. 使用嵌入模型批量向量化所有片段
     * 4. 存入 InMemoryEmbeddingStore 并返回
     *
     * @param documentPath 文档路径
     * @param embeddingModel 用于向量化的模型
     * @return 包含文档向量索引的内存存储
     */
    private static EmbeddingStore<TextSegment> embed(Path documentPath, EmbeddingModel embeddingModel) {
        DocumentParser documentParser = new TextDocumentParser();
        Document document = loadDocument(documentPath, documentParser);

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
        return embeddingStore;
    }
}