package _03_advanced;

import _02_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 高级 RAG 示例 —— 重排序（Re-Ranking）
 *
 * 核心问题：
 * 向量检索（Embedding-based Retrieval）虽然速度快、成本低，
 * 但检索质量往往不够理想。原因包括：
 * - 嵌入模型对长文本的理解有限，可能捕获到表面语义而非真实相关性
 * - 向量相似度（余弦距离）是"粗粒度"匹配，无法精细判断查询与文档的深层关联
 * - 召回的 Top-K 结果中常混入"伪相关"片段（表面相似但内容无关）
 *
 * 后果：
 * 将不相关的片段塞给 LLM，不仅浪费 Token（成本高），
 * 还可能导致模型产生幻觉（Hallucination），生成错误回答。
 *
 * 解决方案 —— 两阶段检索：
 * 第一阶段（召回）：使用快速、低成本的嵌入模型做向量检索，召回较多候选（如 Top-20）
 * 第二阶段（精排）：使用更强大的重排序模型对候选结果重新打分，过滤低质量片段，只保留真正相关的
 *
 * 本示例使用 Cohere Rerank API 作为重排序模型。
 */
public class _03_Advanced_RAG_with_ReRanking_Example {

    public static void main(String[] args) {

        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        /**
         * 启动对话，观察重排序的过滤效果：
         *
         * 第一轮：说 "Hi"
         *   → 向量检索会召回一些片段（因为 "Hi" 与文档中的某些词有表面相似性）
         *   → 但重排序模型会判断这些片段与 "Hi" 不相关，全部过滤掉
         *   → LLM 只能基于自身知识回答，不会引用文档
         *
         * 第二轮：问 "Can I cancel my reservation?"
         *   → 向量检索召回 5 个候选片段
         *   → 重排序后可能只剩 1 个真正相关的（取消政策条款）
         *   → LLM 基于精准片段生成准确回答
         */
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // ==================== 第一步：加载并处理文档 ====================

        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        /**
         * 使用 EmbeddingStoreIngestor 构建摄入流水线。
         * 自动完成：分割 → 嵌入 → 存储。
         */
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        // ==================== 第二阶段：配置内容检索器（第一阶段 - 召回）====================

        /**
         * 配置向量检索器，作为第一阶段召回。
         *
         * 关键调整：
         * - maxResults(5)：召回 5 个候选（比朴素 RAG 的 2 个多，给重排序留足选择空间）
         * - 不设置 minScore：第一阶段不过滤，避免误杀潜在相关片段
         *
         * 策略：第一阶段"宽召回"，第二阶段"严过滤"。
         */
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5) // 召回更多候选，供后续重排序筛选
                .build();

        // ==================== 第二阶段：配置重排序模型（第二阶段 - 精排）====================

        /**
         * 创建 Cohere 重排序模型。
         *
         * Cohere Rerank 是专门训练的重排序模型，特点：
         * - 输入：查询 + 候选文档列表
         * - 输出：每个文档的相关性分数（0~1）
         * - 优势：比向量相似度更精准，能理解查询与文档的深层语义关联
         *
         * 注册获取免费 API Key：https://dashboard.cohere.com/welcome/register
         *
         * 模型选择：
         * - rerank-multilingual-v3.0：支持多语言（本示例使用）
         * - rerank-english-v3.0：英文专用，效果可能略好
         *
         * 依赖要求：需要引入 langchain4j-cohere 模块。
         */
        ScoringModel scoringModel = CohereScoringModel.builder()
                .apiKey(System.getenv("COHERE_API_KEY"))
                .modelName("rerank-multilingual-v3.0")
                .build();

        /**
         * 创建重排序内容聚合器。
         *
         * 工作流程：
         * 1. 接收第一阶段召回的候选片段列表
         * 2. 将用户查询 + 每个片段一起发送给 Cohere Rerank
         * 3. Cohere 返回每个片段的相关性分数
         * 4. 过滤掉分数低于 minScore 的片段
         * 5. 按分数降序排列，返回给 LLM
         *
         * minScore(0.8) 设置较高的阈值，确保只有"真正相关"的片段才会进入 LLM 的上下文。
         */
        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel)
                .minScore(0.8) // 严格过滤，只保留高置信度的相关片段
                .build();

        // ==================== 第三步：组装检索增强器 ====================

        /**
         * DefaultRetrievalAugmentor 组合检索器和聚合器。
         *
         * 执行流程：
         * 1. ContentRetriever 向量召回 Top-5 候选
         * 2. ContentAggregator（ReRankingContentAggregator）调用 Cohere 重排序
         * 3. 过滤后可能只剩 0~2 个高质量片段
         * 4. 将最终片段拼接到 Prompt，发给 LLM
         */
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(contentAggregator)
                .build();

        // ==================== 第四步：创建对话模型并组装助手 ====================

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
}