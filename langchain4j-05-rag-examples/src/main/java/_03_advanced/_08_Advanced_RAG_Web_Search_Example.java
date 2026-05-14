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
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import shared.Assistant;

import java.nio.file.Path;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 高级 RAG 示例 —— 结合网络搜索（Web Search）
 *
 * 核心场景：
 * 本地知识库（向量存储）的信息是有限的、静态的，可能无法覆盖用户的所有问题。
 * 当用户询问本地知识库之外的内容时，需要借助网络搜索获取实时、全面的信息。
 *
 * 例如：
 * - "Can I cancel my reservation?" → 本地租车条款文档可回答
 * - "What is the latest news about electric vehicles?" → 需要网络搜索获取最新资讯
 *
 * 解决方案：
 * 将本地向量检索与网络搜索并行作为两个 ContentRetriever，
 * 用户查询同时发送给两者，合并结果后交给 LLM 综合回答。
 *
 * 依赖要求：
 * 需要引入 langchain4j-web-search-engine-tavily 模块。
 */
public class _08_Advanced_RAG_Web_Search_Example {

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        /**
         * 启动对话，测试混合检索效果：
         *
         * 本地知识库相关的问题：
         * - "Can I cancel my reservation?" → 向量检索返回租车条款片段
         * - "What is the liability policy?" → 向量检索返回责任条款片段
         *
         * 需要网络搜索的问题：
         * - "What is the latest news about Tesla?" → 网络搜索返回最新资讯
         * - "How does electric car rental work?" → 网络搜索返回行业动态
         *
         * 模糊问题可能同时触发两者：
         * - "Tell me about car rental trends" → 本地条款 + 网络趋势合并回答
         */
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // ==================== 第一步：创建本地向量检索器 ====================

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        /**
         * 加载租车公司服务条款文档，构建本地向量存储。
         * embed() 是本地辅助方法，封装了完整的文档处理流水线。
         */
        EmbeddingStore<TextSegment> embeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);

        /**
         * 本地内容检索器：EmbeddingStoreContentRetriever
         *
         * 作用：从本地文档向量存储中检索与用户查询语义相关的片段。
         * 适用：回答基于企业内部文档、固定知识库的问题。
         */
        ContentRetriever embeddingStoreContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // ==================== 第二步：创建网络搜索检索器 ====================

        /**
         * 配置 Tavily 网络搜索引擎。
         *
         * Tavily 是什么？
         * - 专为 AI 应用优化的搜索引擎
         * - 返回结构化的搜索结果（标题、摘要、URL、内容）
         * - 支持实时搜索，覆盖全网最新信息
         * - 比传统搜索引擎更适合 LLM 的 RAG 场景
         *
         * 获取免费 API Key：https://app.tavily.com/sign-in
         */
        WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
                .apiKey(System.getenv("TAVILY_API_KEY")) // 从环境变量读取 API Key
                .build();

        /**
         * 网络内容检索器：WebSearchContentRetriever
         *
         * 作用：将用户查询发送给 Tavily 搜索引擎，获取实时网络结果。
         * 适用：回答需要最新信息、本地知识库未覆盖的问题。
         *
         * maxResults(3)：最多返回 3 条搜索结果。
         *
         * 结果格式：每条结果包含标题、摘要、URL 和内容，
         * 框架会自动将其转换为 TextSegment 注入 Prompt。
         */
        ContentRetriever webSearchContentRetriever = WebSearchContentRetriever.builder()
                .webSearchEngine(webSearchEngine)
                .maxResults(3)
                .build();

        // ==================== 第三步：创建查询路由器 ====================

        /**
         * DefaultQueryRouter：将查询同时广播给本地检索器和网络搜索检索器。
         *
         * 执行流程：
         * 1. 用户查询并行发送给 embeddingStoreContentRetriever 和 webSearchContentRetriever
         * 2. 本地检索器从向量存储搜索相关文档片段
         * 3. 网络检索器调用 Tavily API 搜索互联网
         * 4. 合并所有结果（默认去重、按相关性排序）
         * 5. 将合并后的上下文拼接到 Prompt
         *
         * 与智能路由的区别：
         * - 本示例使用 DefaultQueryRouter，无条件广播
         * - 也可用 LanguageModelQueryRouter 智能判断：本地问题走向量库，时事问题走网络搜索
         */
        QueryRouter queryRouter = new DefaultQueryRouter(embeddingStoreContentRetriever, webSearchContentRetriever);

        // ==================== 第四步：组装检索增强器 ====================

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        // ==================== 第五步：创建对话模型并组装助手 ====================

        ChatModel model = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(model)
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