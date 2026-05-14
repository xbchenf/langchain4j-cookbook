package _03_advanced;

import _02_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 高级 RAG 示例 —— 查询路由（Query Routing）
 *
 * 请先阅读 {@link Naive_RAG_Example} 了解基础 RAG 流程。
 *
 * 核心问题：
 * 企业私有数据往往分散在多个来源和格式中：
 * - Confluence 上的内部文档
 * - Git 仓库中的项目代码
 * - 关系型数据库中的用户数据
 * - 搜索引擎中的商品信息
 * ...
 *
 * 如果为每个数据源都建立一个向量存储/检索器，
 * 朴素的做法是把用户查询广播给所有检索器，然后合并结果。
 * 但这会带来两个问题：
 * 1. 效率低：无关检索器白白消耗计算资源
 * 2. 效果差：无关数据混入上下文，干扰 LLM 生成准确回答
 *
 * 解决方案 —— 查询路由：
 * 根据查询内容，智能选择最合适的 1 个或多个检索器。
 *
 * 路由策略有多种实现方式：
 * - 规则路由：根据用户权限、地理位置等硬规则
 * - 关键词路由：查询包含特定关键词时路由到对应检索器
 * - 语义路由：用嵌入模型计算查询与各类别的语义相似度
 * - LLM 路由：让大模型判断查询应该交给哪个检索器（本示例采用）
 *
 * 前三种策略可自定义实现 QueryRouter 接口。
 * 第四种 LangChain4j 已内置 LanguageModelQueryRouter。
 */
public class _02_Advanced_RAG_with_Query_Routing_Example {

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        /**
         * 启动对话。建议按以下顺序测试，观察日志中的路由决策：
         *
         * 第一轮：问 "What is the legacy of John Doe?"
         *   → 涉及人物传记，应路由到 biographyContentRetriever
         *
         * 第二轮：问 "Can I cancel my reservation?"
         *   → 涉及租车条款，应路由到 termsOfUseContentRetriever
         *
         * 查看日志可看到 LanguageModelQueryRouter 的路由决策过程。
         */
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // ==================== 第一步：准备嵌入模型 ====================

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // ==================== 第二步：构建传记检索器 ====================

        /**
         * 为约翰·多伊的传记文档创建独立的向量存储和检索器。
         *
         * embed() 是本地辅助方法，封装了完整的文档处理流水线：
         * 加载 → 解析 → 分割 → 嵌入 → 存储
         */
        EmbeddingStore<TextSegment> biographyEmbeddingStore =
                embed(toPath("documents/biography-of-john-doe.txt"), embeddingModel);

        ContentRetriever biographyContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(biographyEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // ==================== 第三步：构建租车条款检索器 ====================

        /**
         * 为汽车租赁公司的服务条款创建独立的向量存储和检索器。
         *
         * 两个检索器完全隔离，各自维护自己的嵌入空间和索引，
         * 避免不同领域的数据互相污染。
         */
        EmbeddingStore<TextSegment> termsOfUseEmbeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);

        ContentRetriever termsOfUseContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(termsOfUseEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // ==================== 第四步：创建对话模型 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        // ==================== 第五步：创建查询路由器（核心）====================

        /**
         * LanguageModelQueryRouter：基于 LLM 的智能查询路由器。
         *
         * 工作原理：
         * 1. 为每个 ContentRetriever 提供自然语言描述
         * 2. 用户提问时，将问题 + 所有检索器描述一起发给 LLM
         * 3. LLM 判断该查询应该路由到哪些检索器
         * 4. 返回对应的 ContentRetriever 列表（可能 1 个，也可能多个）
         *
         * 描述质量直接影响路由准确性：
         * - 描述应清晰区分不同检索器的覆盖范围
         * - 避免模糊或重叠的描述
         */
        Map<ContentRetriever, String> retrieverToDescription = new HashMap<>();
        retrieverToDescription.put(biographyContentRetriever, "biography of John Doe");
        retrieverToDescription.put(termsOfUseContentRetriever, "terms of use of car rental company");

        QueryRouter queryRouter = new LanguageModelQueryRouter(chatModel, retrieverToDescription);

        // ==================== 第六步：构建检索增强器 ====================

        /**
         * DefaultRetrievalAugmentor 配置 queryRouter。
         *
         * 执行流程（每次用户提问时）：
         * 1. QueryRouter 接收用户查询
         * 2. LLM 判断查询意图，选择最合适的 ContentRetriever（1 个或多个）
         * 3. 被选中的检索器各自执行向量搜索
         * 4. 合并所有检索结果（默认去重、按分数排序）
         * 5. 将结果片段拼接到 System Prompt，发给 LLM 生成回答
         *
         * 注意：这里没有显式配置 queryTransformer 或 contentAggregator，
         * 使用默认实现（无查询变换，简单合并结果）。
         */
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        // ==================== 第七步：组装 AI Service ====================

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