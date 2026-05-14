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
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.nio.file.Path;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 高级 RAG 示例 —— 多检索器并行检索（Multiple Content Retrievers）
 *
 * 核心场景：
 * 当知识库包含多个不同领域的文档时，可能需要同时从多个来源检索信息。
 * 例如：用户问题可能同时涉及公司条款和人物传记，需要合并两个知识库的结果。
 *
 * 与查询路由（Query Routing）的区别：
 * - 查询路由：智能选择 1 个或多个最合适的检索器（选择性）
 * - 多检索器：无条件地将查询广播给所有检索器，合并结果（全量性）
 *
 * 适用场景：
 * - 文档数量少、领域关联紧密，不需要智能路由
 * - 希望简单暴力地覆盖所有知识库，避免遗漏
 * - 作为查询路由的基准对比（评估路由带来的收益）
 */
public class _07_Advanced_RAG_Multiple_Retrievers_Example {

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        /**
         * 启动对话，测试多检索器效果：
         *
         * 可以问跨领域的问题，例如：
         * - "What is the cancellation policy and who is John Doe?"
         *   → 会同时从租车条款和人物传记两个知识库检索片段
         *   → LLM 基于合并后的上下文综合回答
         *
         * 也可以问单领域问题：
         * - "Can I cancel my reservation?" → 条款库有结果，传记库无相关结果
         * - "What is John Doe's legacy?" → 传记库有结果，条款库无相关结果
         */
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // ==================== 第一步：准备嵌入模型 ====================

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // ==================== 第二步：创建第一个检索器（租车条款）====================

        /**
         * 为 "Miles of Smiles" 租车公司的服务条款创建向量存储和检索器。
         */
        EmbeddingStore<TextSegment> embeddingStore1 =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);

        ContentRetriever contentRetriever1 = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore1)
                .embeddingModel(embeddingModel)
                .maxResults(2)      // 每个检索器最多返回 2 条
                .minScore(0.6)      // 相似度阈值
                .build();

        // ==================== 第三步：创建第二个检索器（人物传记）====================

        /**
         * 为约翰·多伊的传记创建独立的向量存储和检索器。
         *
         * 两个检索器完全独立，各自维护自己的向量空间和索引。
         */
        EmbeddingStore<TextSegment> embeddingStore2 =
                embed(toPath("documents/biography-of-john-doe.txt"), embeddingModel);

        ContentRetriever contentRetriever2 = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore2)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // ==================== 第四步：创建默认查询路由器（核心）====================

        /**
         * DefaultQueryRouter：默认查询路由器，将所有查询广播给所有检索器。
         *
         * 与 LanguageModelQueryRouter 的区别：
         * - DefaultQueryRouter：无脑广播，不判断查询意图，所有检索器都执行
         * - LanguageModelQueryRouter：智能选择，只路由到最相关的检索器
         *
         * 执行流程：
         * 1. 用户查询同时发送给 contentRetriever1 和 contentRetriever2
         * 2. 两个检索器各自独立执行向量搜索
         * 3. 合并所有结果（默认去重、按分数排序）
         * 4. 将合并后的片段拼接到 Prompt
         *
         * 结果合并策略：
         * - 如果两个检索器都返回结果，LLM 看到 4 个片段（各 2 个）
         * - 如果只有一个有结果，LLM 看到 2 个片段
         * - 如果都无结果，LLM 基于自身知识回答
         */
        QueryRouter queryRouter = new DefaultQueryRouter(contentRetriever1, contentRetriever2);

        // ==================== 第五步：组装检索增强器 ====================

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        // ==================== 第六步：创建对话模型并组装助手 ====================

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