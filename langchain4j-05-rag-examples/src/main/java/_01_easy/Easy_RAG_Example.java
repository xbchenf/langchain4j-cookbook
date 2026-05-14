package _01_easy;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import _02_naive.Naive_RAG_Example;
import shared.Assistant;

import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;
import static shared.Utils.*;

/**
 * Easy RAG（简易检索增强生成）示例
 *
 * 本示例演示如何用 LangChain4j 的"简易模式"快速搭建 RAG 应用。
 * 所谓"简易"，是指所有底层细节（文档解析、文本分割、向量化、存储）都被框架封装，
 * 开发者只需几行代码即可完成，无需关心内部实现。
 *
 * 对比学习：
 * - 本示例（Easy RAG）：开箱即用，适合快速原型和简单场景
 * - {@link Naive_RAG_Example}：手动拆解每一步，适合深入理解原理
 *
 * RAG 核心流程（本示例中自动完成）：
 * 1. 加载文档 → 2. 文本分割 → 3. 向量化 → 4. 存入向量存储 → 5. 检索相关片段 → 6. 拼接到 Prompt → 7. LLM 生成回答
 */
public class Easy_RAG_Example {

    private static final ChatModel CHAT_MODEL = OpenAiChatModel.builder()
            .apiKey("demo")
            .modelName("gpt-4o-mini")
            .baseUrl("http://langchain4j.dev/demo/openai/v1")
            .build();

    public static void main(String[] args) {

        // ==================== 第一步：加载文档 ====================

        /**
         * 从文件系统加载文档。
         *
         * toPath("documents/"): 指定文档目录路径
         * glob("*.txt"): 只加载 .txt 后缀的文件
         *
         * loadDocuments 返回 List<Document>，每个 Document 包含：
         * - text: 文件完整文本内容
         * - metadata: 元数据（如文件路径、文件名等）
         */
        List<Document> documents = loadDocuments(toPath("documents/"), glob("*.txt"));

        // ==================== 第二步：构建 AI 助手 ====================

        /**
         * 使用 AiServices 构建具备 RAG 能力的助手。
         *
         * 配置说明：
         * - chatModel: 底层大语言模型（GPT-4o-mini）
         * - chatMemory: 对话记忆，保留最近 10 条消息，实现多轮对话上下文
         * - contentRetriever: 内容检索器，从向量存储中查找与用户问题相关的文档片段
         *
         * 运行时流程：
         * 1. 用户提问 → 2. 框架自动将问题向量化
         * 3. 在 embeddingStore 中搜索相似片段 → 4. 将片段拼接到系统 Prompt
         * 5. 发送给 LLM → 6. 返回带引用来源的自然语言回答
         */
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(CHAT_MODEL)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(createContentRetriever(documents))
                .build();

        // ==================== 第三步：启动对话 ====================

        /**
         * 启动交互式对话。
         *
         * 可以问的问题示例（基于 documents/ 目录下的文档内容）：
         * - "Can I cancel my reservation?"（我能取消预订吗？）
         * - "I had an accident, should I pay extra?"（我出了事故，需要额外付费吗？）
         *
         * 框架会自动从加载的文档中检索相关条款，作为上下文提供给 LLM，
         * 使回答基于真实文档而非模型幻觉。
         */
        startConversationWith(assistant);
    }

    // ==================== 创建内容检索器（核心封装）====================

    /**
     * 创建基于向量存储的内容检索器。
     *
     * 这是 Easy RAG 的"魔法"所在——看似只有 3 行代码，内部自动完成了：
     *
     * 1. 文档分割（Document Splitting）
     *    - 将长文档切分为适合向量化的文本片段（TextSegment）
     *    - 默认按段落/句子边界分割，避免语义断裂
     *
     * 2. 文本向量化（Embedding）
     *    - 使用默认的本地嵌入模型（通常是 all-MiniLM-L6-v2）
     *    - 将每个 TextSegment 转换为 384 维向量
     *
     * 3. 向量存储（Vector Store）
     *    - 使用 InMemoryEmbeddingStore（内存存储）
     *    - 适合数据量小的场景，重启后数据丢失
     *
     * 4. 检索器封装（ContentRetriever）
     *    - 对外暴露统一的 retrieve(Query) 接口
     *    - 内部自动将查询向量化并执行相似度搜索
     *
     * @param documents 原始文档列表
     * @return 封装好的内容检索器，可直接注入 AiServices
     */
    private static ContentRetriever createContentRetriever(List<Document> documents) {

        // 创建空的内存向量存储
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        /**
         * 一键摄入文档。
         *
         * EmbeddingStoreIngestor.ingest() 是 Easy RAG 的核心 API，
         * 自动完成：分割 → 向量化 → 存储 的全流程。
         *
         * 默认配置：
         * - 分割策略：按 300 tokens 左右分段，重叠 30 tokens
         * - 嵌入模型：本地 all-MiniLM-L6-v2（384维）
         * - 存储：内存 HashMap
         *
         * 生产环境建议自定义 Ingestor 配置（如换用更优分割策略、外部向量数据库）。
         */
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        /**
         * 从向量存储创建内容检索器。
         *
         * EmbeddingStoreContentRetriever 是 LangChain4j 的标准检索器实现，
         * 负责将用户的自然语言查询转换为向量，并在存储中搜索最相似的 Top-K 片段。
         *
         * 默认返回 3 条最相关片段，可通过 .maxResults() 调整。
         */
        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }
}