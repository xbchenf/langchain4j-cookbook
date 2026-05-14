package _02_naive;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static shared.Utils.*;

/**
 * 朴素 RAG（检索增强生成）示例 —— 手动拆解完整流程
 *
 * 所谓"朴素"（Naive），是指不使用任何高级 RAG 优化技术（如查询重写、重排序、HyDE 等），
 * 仅展示最基础的 RAG 流水线：加载 → 分割 → 嵌入 → 存储 → 检索 → 生成。
 *
 * 每轮对话的执行流程：
 * 1. 接收用户原始查询（不做任何改写）
 * 2. 使用嵌入模型将查询转换为向量
 * 3. 在向量存储中搜索最相关的 X 个文本片段
 * 4. 将检索到的片段追加到用户查询中，构建增强 Prompt
 * 5. 将组合后的输入发送给大语言模型生成回答
 * 6. 隐含假设：
 *    - 用户查询本身表述清晰、包含足够的检索关键词
 *    - 检索到的片段确实与用户问题相关
 *
 * 与 Easy RAG 的区别：
 * - Easy RAG：一行代码 EmbeddingStoreIngestor.ingest() 搞定所有预处理
 * - Naive RAG：手动展示每个步骤，便于理解原理和自定义调优
 */
public class Naive_RAG_Example {

    public static void main(String[] args) {

        // ==================== 创建具备 RAG 能力的助手 ====================

        /**
         * 创建助手，传入文档路径。
         * 这里使用一份虚构的汽车租赁公司 "Miles of Smiles" 的服务条款文档。
         */
        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        // ==================== 启动交互式对话 ====================

        /**
         * 开始与助手对话。可以问的问题示例：
         * - "Can I cancel my reservation?"（我能取消预订吗？）
         * - "I had an accident, should I pay extra?"（我出了事故，需要额外付费吗？）
         *
         * 助手会基于加载的文档内容回答，而非依赖模型自身的知识。
         */
        startConversationWith(assistant);
    }

    /**
     * 手动构建 RAG 流水线的完整过程。
     *
     * @param documentPath 文档在 classpath 下的相对路径
     * @return 配置好的 AI 助手
     */
    private static Assistant createAssistant(String documentPath) {

        // ==================== 第一步：创建大语言模型 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        // ==================== 第二步：加载文档 ====================

        /**
         * 加载指定路径的文档。
         *
         * 本例只加载单个文件，但 LangChain4j 支持批量加载多种来源：
         * - 文件系统（FileSystemDocumentLoader）
         * - URL、Amazon S3、Azure Blob Storage、GitHub、腾讯云 COS 等
         *
         * 同时支持多种文档格式解析：
         * - TextDocumentParser（纯文本）
         * - PdfDocumentParser、MsOfficeDocumentParser（Office 文档）等
         *
         * 如果数据源不在内置支持范围内，也可以手动构造 Document 对象。
         */
        DocumentParser documentParser = new TextDocumentParser();
        Document document = loadDocument(toPath(documentPath), documentParser);

        // ==================== 第三步：分割文档 ====================

        /**
         * 将长文档分割为较小的文本片段（TextSegment / Chunk）。
         *
         * 为什么要分割？
         * - LLM 上下文有限，无法一次性塞进整本书
         * - 细粒度片段便于精准检索：用户问"取消政策"时，只召回相关段落
         *
         * DocumentSplitters.recursive(300, 0) 使用递归分割策略：
         * - 首先尝试按段落（空行）分割
         * - 如果段落超过 300 tokens，按换行符递归分割
         * - 如果还超长，按句子分割，最后按单词分割
         * - 第二个参数 0 表示片段之间不重叠（overlap）
         *
         * 生产环境建议根据文档类型调整：
         * - 代码文档：按函数/类分割
         * - Markdown：按标题层级分割
         * - 通用文本：300~500 tokens，重叠 10%~20%
         */
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);

        // ==================== 第四步：文本向量化（Embedding）====================

        /**
         * 将文本片段转换为高维向量（Embedding），用于语义相似度搜索。
         *
         * 使用 BgeSmallEnV15QuantizedEmbeddingModel：
         * - 本地运行，无需外部 API
         * - 量化版（Quantized），体积小、推理快
         * - 输出 384 维向量
         * - 英文场景效果较好
         *
         * LangChain4j 支持 10+ 家嵌入模型提供商，包括 OpenAI、本地 ONNX、HuggingFace 等。
         *
         * embedAll(segments) 批量向量化，比逐个 embed 效率更高。
         */
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // ==================== 第五步：存入向量存储 ====================

        /**
         * 创建向量存储（Vector Database），保存向量与原文本的映射关系。
         *
         * InMemoryEmbeddingStore：
         * - 纯内存实现，基于 HashMap
         * - 速度快，但应用重启后数据丢失
         * - 适合原型开发和小数据量场景
         *
         * LangChain4j 支持 15+ 种向量数据库：
         * - Qdrant、Milvus、PgVector、Redis、Chroma、OpenSearch 等
         * - 生产环境建议替换为持久化存储
         *
         * addAll(embeddings, segments) 批量写入，保持向量与文本的一一对应。
         */
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        /**
         * 注：上述 3~5 步（分割、嵌入、存储）也可以用 EmbeddingStoreIngestor 一键完成：
         *   EmbeddingStoreIngestor.ingest(document, embeddingStore);
         * 参见 easy.Easy_RAG_Example 或 _01_Advanced_RAG_with_Query_Compression_Example。
         */

        // ==================== 第六步：创建内容检索器 ====================

        /**
         * ContentRetriever 是 RAG 的核心组件，负责根据用户查询检索相关内容。
         *
         * 配置说明：
         * - embeddingStore: 要搜索的向量存储
         * - embeddingModel: 用于将用户查询向量化的模型（必须与文档嵌入使用同一模型）
         * - maxResults(2): 每轮对话最多召回 2 个最相关的片段
         * - minScore(0.5): 相似度阈值，低于 0.5 的片段会被过滤掉
         *
         * 检索流程（每次用户提问时自动执行）：
         * 1. 将用户问题向量化
         * 2. 在 embeddingStore 中计算余弦相似度
         * 3. 返回 Top-2 且分数 ≥ 0.5 的 TextSegment
         */
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.5)
                .build();

        // ==================== 第七步：创建对话记忆 ====================

        /**
         * 配置聊天记忆，让助手支持多轮对话。
         *
         * MessageWindowChatMemory.withMaxMessages(10):
         * - 保留最近 10 条消息（包括用户、AI、系统消息）
         * - 超出后自动移除最早的消息
         * - 按"消息数"限制，简单直观
         *
         * 另一种选择：TokenWindowChatMemory
         * - 按 Token 数量限制，更精确地控制上下文窗口
         * - 适合长文档对话场景
         */
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // ==================== 第八步：组装 AI Service ====================

        /**
         * 使用 AiServices 组装所有组件，构建最终的 AI 助手。
         *
         * 各组件协作流程：
         * 1. 用户输入 → ChatMemory 加载历史上下文
         * 2. ContentRetriever 将用户问题向量化并检索相关文档片段
         * 3. 框架自动构建 System Prompt，包含检索到的片段
         * 4. ChatModel 基于"历史对话 + 系统指令 + 检索片段 + 当前问题"生成回答
         * 5. 回答存入 ChatMemory，等待下一轮对话
         */
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }
}