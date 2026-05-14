package com.langchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * RAG文档加载器测试类
 * 
 * 测试FileSystemDocumentLoader的各种使用方法，包括：
 * - 加载单个文档
 * - 指定文档解析器
 * - 批量加载目录下的文档
 * - 使用通配符过滤特定格式的文档
 */
@SpringBootTest
public class RagTest {

    /**
     * 测试文档加载功能
     * 
     * 演示四种不同的文档加载方式：
     * 1. 加载单个文本文件（使用默认解析器）
     * 2. 加载单个文本文件（指定TextDocumentParser解析器）
     * 3. 批量加载目录下所有文档
     * 4. 使用通配符过滤，只加载特定格式的文档
     */
    @Test
    public void testDocumentLoader() {
        // 获取项目根目录（JUnit测试的工作目录是项目根目录）
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        
        // 构建文档文件的完整路径
        // 注意：文件位于 src/main/resources/documents/test1.txt
        Path docPath = projectRoot.resolve("src/main/resources/documents/test1.txt");
        
        // ==================== 测试1：加载单个文档（使用默认解析器）====================
        // FileSystemDocumentLoader会根据文件扩展名自动选择合适的解析器
        Document document = FileSystemDocumentLoader.loadDocument(docPath);
        System.out.println("【测试1】单个文档内容：");
        System.out.println(document.text());
        System.out.println("---------------------------------1");

        // ==================== 测试2：加载单个文档（指定解析器）====================
        // 显式指定使用TextDocumentParser解析器
        // 适用于需要明确控制解析行为的场景
        Document document2 = FileSystemDocumentLoader.loadDocument(docPath, new TextDocumentParser());
        System.out.println("【测试2】指定解析器加载的文档内容：");
        System.out.println(document2.text());
        System.out.println("---------------------------------2");

        // ==================== 测试3：批量加载目录下所有文档====================
        // 加载documents目录下的所有支持格式的文档
        // 支持的文件格式取决于项目中引入的解析器依赖（如tika、pdfbox、poi等）
        Path docsDir = projectRoot.resolve("src/main/resources/documents");
        List<Document> document3 = FileSystemDocumentLoader.loadDocuments(docsDir);
        System.out.println("【测试3】目录下文档总数：" + document3.size());
        for (int i = 0; i < document3.size(); i++) {
            System.out.println("--- 文档 " + (i + 1) + " ---");
            System.out.println(document3.get(i).text());
        }
        System.out.println("---------------------------------3");

        // ==================== 测试4：使用通配符过滤特定格式====================
        // 只加载.txt格式的文档，忽略其他格式（如.pdf、.docx等）
        // glob:*.txt 表示匹配所有以.txt结尾的文件
        // 其他常用模式：
        //   - glob:*.pdf  （只加载PDF）
        //   - glob:*.docx （只加载Word）
        //   - glob:test*  （只加载以test开头的文件）
        List<Document> document4 = FileSystemDocumentLoader.loadDocuments(
            docsDir, 
            FileSystems.getDefault().getPathMatcher("glob:*.txt")
        );
        System.out.println("【测试4】TXT格式文档数量：" + document4.size());
        for (int i = 0; i < document4.size(); i++) {
            System.out.println("--- TXT文档 " + (i + 1) + " ---");
            System.out.println(document4.get(i).text());
        }
        System.out.println("---------------------------------4");
    }

    /**
     * 测试PDF文档解析
     * 
     * 使用Apache Tika解析器解析PDF文件
     * 注意：PDF文件位于 src/main/resources/documents/test2.pdf
     * 
     * 重要说明：
     * - Document.text() 只返回纯文本内容，不包含格式信息
     * - 表格会被转换为纯文本（可能丢失行列结构）
     * - 图片无法提取（因为图片不是文本）
     * - 如需保留表格结构，建议使用专门的表格提取工具
     */
    @Test
    public void testPdfParser(){
        // 创建Tika文档解析器（支持PDF、Word、Excel等多种格式）
        DocumentParser parser = new ApacheTikaDocumentParser();
        
        // 从classpath加载PDF文件
        // 文件路径：src/main/resources/documents/test2.pdf
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("documents/test2.pdf");
        

        // 解析PDF文档
        Document document = parser.parse(inputStream);
        
        // ==================== 输出解析结果 ====================
        System.out.println("【PDF解析结果】");
        System.out.println("==========================================");
        
        // 1. 文档基本信息
        System.out.println("📄 文档内容长度：" + document.text().length() + " 字符");
        System.out.println("📊 文档元数据：");
        System.out.println(document.metadata());
        System.out.println("------------------------------------------");
        
        // 2. 文档内容预览（前1000字符）
        String fullText = document.text();
        int previewLength = Math.min(1000, fullText.length());
        String preview = fullText.substring(0, previewLength);
        
        System.out.println("📝 文档内容预览（前" + previewLength + "字符）：");
        System.out.println(preview);
        
        if (fullText.length() > 1000) {
            System.out.println("\n...（内容过长，仅显示前1000字符）...");
        }
        
        System.out.println("==========================================");
        
        // 3. 重要提示
        System.out.println("\n⚠️  注意事项：");
        System.out.println("   - 表格已被转换为纯文本，可能丢失行列结构");
        System.out.println("   - 图片无法提取（Document.text()只包含文本）");
        System.out.println("   - 如需处理表格，建议使用专门的表格提取库");
        System.out.println("   - RAG场景中，通常只需要文本内容进行向量化");
    }

    /**
     * embeddingsTest
     */
    @Test
    public void testEmbeddings(){
        // ==================== 第1步：加载文档 ====================
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path docPath = projectRoot.resolve("src/main/resources/documents/test3.txt");
        Document document = FileSystemDocumentLoader.loadDocument(docPath);

        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(document, store);

    }

    /**
     * 测试7种文档分割器的简单使用示例
     */
    @Test
    public void testTextSplitter(){
        // 加载测试文档
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path docPath = projectRoot.resolve("src/main/resources/documents/test3.txt");
        Document document = dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(docPath);

        // 递归分割器- 推荐使用
        DocumentSplitter splitter = DocumentSplitters.recursive(
                300,  // 每块约300字符
                30    // 重叠区防止知识断裂
        );
        List<TextSegment> segments = splitter.split(document); // 文档瞬间变寿司拼盘
        System.out.println(segments);
        System.out.println("==========================================\n");

        // ===== 1. 按段落分割（DocumentByParagraphSplitter）=====
        DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(300, 30);
        List<TextSegment> paragraphSegments = paragraphSplitter.split(document);
        System.out.println(paragraphSegments);

        // ===== 2. 按行分割（DocumentByLineSplitter）=====
        // ===== 3. 按句子分割（DocumentBySentenceSplitter）推荐 =====
        // ===== 4. 按单词分割（DocumentByWordSplitter）=====
        // ===== 5. 按字符分割（DocumentByCharacterSplitter）=====
        // ===== 6. 按正则表达式分割（DocumentByRegexSplitter）=====

    }

    @Autowired
    private EmbeddingModel embeddingModel;
    @Test
    public void testEmbeddingModel() {
        Response<Embedding> res = embeddingModel.embed("你好");
        System.out.println(res.content().vector().length);
        System.out.println(res);
    }

    @Autowired
    private EmbeddingStore embeddingStore;
    @Test
    public void testEmbeddingStore() {
        TextSegment text = TextSegment.from("我喜欢打台球");
        Embedding embedding = embeddingModel.embed(text).content();
        embeddingStore.add(embedding, text);//存入向量数据库
    }

    @Test
    public void testEmbeddingStoreSearch() {
        Response<Embedding> queryEmbedding = embeddingModel.embed("我有什么爱好？");
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding.content())
                .maxResults(1)
                .build();
        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(embeddingSearchRequest);
        EmbeddingMatch<TextSegment> textSegmentEmbeddingMatch = search.matches().get(0);
        System.out.println(textSegmentEmbeddingMatch.embedded().text());
        System.out.println(textSegmentEmbeddingMatch.score());
    }
}
