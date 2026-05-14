package _03_advanced;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
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
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 高级 RAG 示例 —— 查询压缩（Query Compression）
 *
 * 核心问题：
 * 在多轮对话中，用户的后续提问往往是"省略式"的，依赖前文上下文。
 * 例如：
 *   用户：约翰·多伊的遗产是什么？
 *   AI：约翰·多伊是一位...
 *   用户：他什么时候出生的？
 *
 * 此时如果用朴素 RAG，检索的查询是"他什么时候出生的？"，向量存储中没有"他"的指代信息，
 * 导致检索失败或召回无关内容。
 *
 * 解决方案 —— 查询压缩：
 * 将用户当前问题 + 历史对话一起交给 LLM，让 LLM 压缩成一个"自包含"的独立查询。
 * 例如将"他什么时候出生的？"压缩为"约翰·多伊什么时候出生的？"。
 *
 * 代价与收益：
 * - 代价：增加一次 LLM 调用，带来额外延迟和 Token 成本
 * - 收益：检索质量显著提升，多轮对话体验更连贯
 *
 * 优化提示：用于压缩的 LLM 不必与对话 LLM 相同，
 * 可以使用更轻量的本地模型（如专门微调的摘要模型）来降低成本。
 */
public class _01_Advanced_RAG_with_Query_Compression_Example {

    public static void main(String[] args) {

        // 创建助手，加载约翰·多伊的传记文档
        Assistant assistant = createAssistant("documents/biography-of-john-doe.txt");

        /**
         * 启动对话。建议按以下顺序测试，观察日志中的查询压缩效果：
         *
         * 第一轮：问 "What is the legacy of John Doe?"
         *   → 这是对话开头，没有前文，查询不会被压缩（或压缩后与原句一致）
         *
         * 第二轮：问 "When was he born?"
         *   → 有前文上下文，CompressingQueryTransformer 会将查询压缩为类似
         *     "When was John Doe born?" 的自包含查询，从而精准召回传记中的出生日期片段
         */
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // ==================== 第一步：加载文档 ====================

        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        // ==================== 第二步：配置嵌入模型与存储 ====================

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        /**
         * 使用 EmbeddingStoreIngestor 的 Builder 模式构建文档摄入流水线。
         *
         * 相比手动执行 split → embed → add，Ingestor 将流程内聚为一个可复用的管道：
         * - documentSplitter: 文档分割策略（递归分割，每段约 300 tokens，无重叠）
         * - embeddingModel: 用于向量化的模型
         * - embeddingStore: 目标向量存储
         *
         * 这种声明式配置便于统一管理和复用。
         */
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // 执行摄入：将文档经过分割、嵌入后存入向量存储
        ingestor.ingest(document);

        // ==================== 第三步：创建对话模型 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        // ==================== 第四步：创建查询压缩器（核心）====================

        /**
         * CompressingQueryTransformer：查询压缩转换器。
         *
         * 工作原理：
         * 每次用户提问时，它会将以下信息拼接后发送给配置的 LLM：
         * - 历史对话记录（从 ChatMemory 获取）
         * - 当前用户查询
         *
         * 要求 LLM 输出一个"独立查询"（stand-alone query），
         * 即不依赖上下文也能明确表达用户意图的查询句。
         *
         * 例如输入：
         *   历史：User: What is the legacy of John Doe? AI: ...
         *   当前：When was he born?
         * 输出：
         *   When was John Doe born?
         *
         * 这个压缩后的查询才会被用于向量检索。
         */
        QueryTransformer queryTransformer = new CompressingQueryTransformer(chatModel);

        // ==================== 第五步：创建内容检索器 ====================

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6) // 提高了相似度阈值，过滤低质量片段
                .build();

        // ==================== 第六步：构建检索增强器（RAG 入口）====================

        /**
         * RetrievalAugmentor 是 LangChain4j 中 RAG 流程的统一入口。
         *
         * 与朴素 RAG 的区别：
         * - 朴素 RAG：直接把 ContentRetriever 交给 AiServices，流程固定（查询→检索→拼接）
         * - 高级 RAG：通过 RetrievalAugmentor 组合多个组件，可自定义查询变换、检索、重排序、聚合等全链路
         *
         * DefaultRetrievalAugmentor 是标准实现，支持配置：
         * - queryTransformer: 查询变换（本例使用压缩）
         * - contentRetriever: 内容检索
         * - contentAggregator: 结果聚合（去重、重排序等，本例未配置，使用默认）
         *
         * 执行顺序（每次用户提问时）：
         * 1. QueryTransformer 处理查询（压缩）
         * 2. ContentRetriever 用处理后的查询检索片段
         * 3. （可选）ContentAggregator 聚合过滤片段
         * 4. 将最终片段拼接到 System Prompt，发给 LLM 生成回答
         */
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer)
                .contentRetriever(contentRetriever)
                .build();

        // ==================== 第七步：组装 AI Service ====================

        /**
         * 构建助手。
         *
         * 注意：这里使用的是 .retrievalAugmentor() 而非 .contentRetriever()，
         * 这意味着 AiServices 会将整个 RAG 流程委托给 RetrievalAugmentor 处理，
         * 从而启用查询压缩等高级能力。
         */
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}