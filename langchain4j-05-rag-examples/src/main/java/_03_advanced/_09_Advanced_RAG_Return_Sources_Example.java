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
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.OPENAI_API_KEY;
import static shared.Utils.toPath;

/**
 * 高级 RAG 示例 —— 返回检索来源（Return Sources）
 *
 * 核心场景：
 * RAG 应用不仅要生成回答，还需要向用户展示"这个回答基于哪些资料"，
 * 即回答的可追溯性和可信度证明。这在法律、医疗、金融等对准确性要求高的领域尤为重要。
 *
 * 解决方案：
 * 使用 AiServices 的 Result<T> 返回类型，框架会自动将检索到的源内容（sources）
 * 与生成结果一起返回，便于展示引用、溯源或审计。
 *
 * Result 对象包含：
 * - content(): LLM 生成的最终回答文本
 * - sources(): 检索阶段使用的原始内容片段列表（即 RAG 的"证据"）
 */
public class _09_Advanced_RAG_Return_Sources_Example {

    /**
     * 自定义助手接口，返回类型为 Result<String> 而非普通 String。
     *
     * Result<T> 是 LangChain4j 的特殊返回类型，用于获取完整的 RAG 执行结果，
     * 包括生成内容和检索来源。
     */
    interface Assistant {

        /**
         * 回答用户查询，同时返回检索来源。
         *
         * @param query 用户问题
         * @return Result 对象，包含回答内容和检索来源
         */
        Result<String> answer(String query);
    }

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        /**
         * 自定义交互循环，展示如何获取和使用 sources。
         *
         * 与 shared.Utils.startConversationWith() 的区别：
         * - 官方工具方法只打印回答，不展示来源
         * - 本示例手动实现循环，额外打印 sources 供用户查看
         */
        Logger log = LoggerFactory.getLogger(shared.Assistant.class);
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                log.info("==================================================");
                log.info("User: ");
                String userQuery = scanner.nextLine();
                log.info("==================================================");

                if ("exit".equalsIgnoreCase(userQuery)) {
                    break;
                }

                // ==================== 调用助手并获取完整结果 ====================

                /**
                 * 调用 assistant.answer()，返回 Result<String> 而非普通 String。
                 *
                 * Result 对象结构：
                 * - result.content(): String 类型，LLM 生成的回答文本
                 * - result.sources(): List<Content> 类型，检索到的原始片段列表
                 * - result.tokenUsage(): Token 使用量统计（可选）
                 * - result.finishReason(): 生成结束原因（可选）
                 */
                Result<String> result = assistant.answer(userQuery);

                // 打印 LLM 生成的回答
                log.info("==================================================");
                log.info("Assistant: " + result.content());

                // ==================== 打印检索来源（核心）====================

                /**
                 * 获取并展示检索来源。
                 *
                 * sources 列表中的每个 Content 对象包含：
                 * - textSegment: 原始文本片段内容
                 * - textSegment.metadata(): 片段的元数据（如文件名、索引等）
                 *
                 * 应用场景：
                 * - 在 UI 中展示引用标注（如 [1], [2]）
                 * - 提供"查看原文"链接
                 * - 审计追踪：记录每次回答基于哪些文档片段
                 * - 调试优化：分析检索质量，调整分割策略或相似度阈值
                 */
                log.info("Sources: ");
                List<Content> sources = result.sources();
                sources.forEach(content -> log.info(content.toString()));
            }
        }
    }

    private static Assistant createAssistant() {

        // ==================== 第一步：创建本地向量检索器 ====================

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // ==================== 第二步：创建对话模型 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        // ==================== 第三步：组装 AI Service ====================

        /**
         * 构建助手，使用 Result<String> 作为返回类型。
         *
         * 框架内部处理：
         * 1. 用户查询 → 2. 向量检索召回片段 → 3. 片段注入 Prompt → 4. LLM 生成回答
         * 5. 将生成的回答和检索到的 sources 一起封装为 Result 对象返回
         *
         * 注意：即使使用 Result 返回类型，其他配置（chatModel, contentRetriever, chatMemory）
         * 与普通 String 返回类型完全一致。
         */
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
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