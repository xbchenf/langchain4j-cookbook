package _03_advanced;

import _02_naive.Naive_RAG_Example;
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
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.Arrays.asList;
import static shared.Utils.*;

/**
 * 高级 RAG 示例 —— 元数据注入（Metadata Injection）
 *
 * 核心问题：
 * 朴素 RAG 只将检索到的文本片段内容拼接到 Prompt 中，
 * LLM 不知道这些片段来自哪里（哪个文件、第几页、什么类别）。
 * 但很多时候，来源信息本身就是答案的一部分，或者对验证回答可信度至关重要。
 *
 * 例如用户问："取消政策定义在哪个文件里？"
 * 如果只给 LLM 片段内容，它无法知道文件名；
 * 但如果同时注入 file_name 元数据，LLM 就能准确回答。
 *
 * 解决方案 —— 内容注入器（ContentInjector）：
 * 自定义检索片段在 Prompt 中的呈现格式，将元数据（如文件名、页码、索引等）
 * 与片段内容一起注入 LLM 的上下文。
 */
public class _04_Advanced_RAG_with_Metadata_Example {

    public static void main(String[] args) {

        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        /**
         * 启动对话，测试元数据注入效果：
         *
         * 问："What is the name of the file where cancellation policy is defined?"
         * （取消政策定义在哪个文件里？）
         *
         * 观察日志可看到：
         * - 检索到的片段内容被注入 Prompt
         * - 同时附带了 file_name 和 index 元数据
         * - LLM 基于 file_name 元数据，能准确回答文件名
         */
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // ==================== 第一步：加载并处理文档 ====================

        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        /**
         * 文档摄入流水线。
         *
         * 注意：LangChain4j 的文档加载器会自动将文件路径等信息存入 Document 的 metadata 中。
         * 分割后，这些元数据会继承到每个 TextSegment 上。
         * 默认包含的元数据键：
         * - file_name: 文件名
         * - absolute_directory_path: 绝对目录路径
         * - index: 片段在文档中的序号
         */
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        // ==================== 第二步：配置内容检索器 ====================

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

        // ==================== 第三步：配置内容注入器（核心）====================

        /**
         * 创建自定义内容注入器。
         *
         * ContentInjector 负责决定"检索到的片段如何呈现给 LLM"。
         *
         * DefaultContentInjector 是标准实现，默认行为：
         * - 将每个片段的文本内容直接拼接到 System Prompt
         * - 不显示任何元数据
         *
         * 通过 metadataKeysToInclude() 可以指定要额外注入的元数据字段。
         *
         * 本示例配置：
         * - file_name: 文件名，回答"来自哪个文件"类问题时必需
         * - index: 片段序号，便于追溯和排序
         *
         * 可选配置：
         * - promptTemplate(): 完全自定义片段在 Prompt 中的格式模板
         *   例如可以改成 Markdown 引用格式、JSON 格式等
         */
        ContentInjector contentInjector = DefaultContentInjector.builder()
                // .promptTemplate(...) // 也可以自定义格式模板
                .metadataKeysToInclude(asList("file_name", "index"))
                .build();

        // ==================== 第四步：组装检索增强器 ====================

        /**
         * DefaultRetrievalAugmentor 组合检索器和注入器。
         *
         * 执行流程：
         * 1. ContentRetriever 召回相关片段
         * 2. ContentInjector 将片段内容 + 指定元数据格式化为 Prompt
         * 3. LLM 基于增强后的 Prompt 生成回答
         *
         * 注入后的 Prompt 片段示例：
         *   file_name: miles-of-smiles-terms-of-use.txt
         *   index: 3
         *   content: 4.1 Reservations can be cancelled up to 7 days...
         */
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector)
                .build();

        // ==================== 第五步：创建对话模型并组装助手 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .logRequests(true) // 开启请求日志，可观察注入后的完整 Prompt
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}