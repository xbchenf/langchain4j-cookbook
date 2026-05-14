package com.langchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class MTRagTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore embeddingStore;

    // 定义批次大小，根据报错信息，这里不能超过 10
    private static final int BATCH_SIZE = 10;

    @Test
    public void testLoadMTResources() throws URISyntaxException {
        Path docPath = Paths.get(MTRagTest.class.getClassLoader().getResource("documents/MT.txt").toURI());
        DocumentParser documentParser = new TextDocumentParser();//文档解析器
        //加载解析文档
        Document document=FileSystemDocumentLoader.loadDocument(docPath, documentParser);
        //文档分割
        MyDocumentSplitter documentSplitter = new MyDocumentSplitter();
        List<TextSegment> splitLText =documentSplitter.split(document);
        
        System.out.println("总共分割成 " + splitLText.size() + " 个片段");
        
        // 分批处理，避免超过 API 限制
        int totalSize = splitLText.size();
        for (int i = 0; i < totalSize; i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, totalSize);
            List<TextSegment> batchSegments = splitLText.subList(i, endIndex);
            
            // 向量化当前批次
            Response<List<Embedding>> listResponse = embeddingModel.embedAll(batchSegments);
            
            // 将向量数据和对应的文档存入向量数据库
            embeddingStore.addAll(listResponse.content(), batchSegments);
            
            System.out.println(String.format("已处理 %d-%d/%d 个片段", i + 1, endIndex, totalSize));
        }
        
        System.out.println("所有文档已向量化并存入向量数据库。");
    }

    @Test
    public void testEmbeddingSearch() {
        Response<Embedding> queryEmbedding = embeddingModel.embed("提现时提示银行卡信息错误怎么办？");
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding.content())
                .maxResults(1)
                .minScore(0.7)
                .build();
        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(embeddingSearchRequest);
        EmbeddingMatch<TextSegment> textSegmentEmbeddingMatch = search.matches().get(0);
        System.out.println(textSegmentEmbeddingMatch.embedded().text());
        System.out.println(textSegmentEmbeddingMatch.score());
    }

    @Autowired
    private MTAIService mTAIService;
    @Test
    public void testEmbeddingSearchAIService() {
        String result1 =mTAIService.sendMessage("1","我今天申请退款多久能到账？");
        System.out.println(result1);

        String result2 =mTAIService.sendMessage("1","具体什么日期？");
        System.out.println(result2);

        /**输出
         * 您的退款通常会在1到7个工作日内原路退回至您的支付账户。具体的到账时间还需要根据银行或支付平台的处理时间来确定。如果您在7个工作日后仍未收到退款，请联系我们的客服进行咨询。
         * 计算指定天数后的具体日期
         * 您的退款预计最晚将在7个工作日后到账，根据当前日期推算，那将是2026年05月21日。请注意，这只是一个预估的最后期限，实际到账时间可能会早于这个日期，具体取决于银行或支付平台的处理速度。如果超过这个日期您还没有收到退款，请随时联系我们客服进行查询。
         *
         * （注：这里提供的日期是基于计算得出的理论上的未来日期，实际情况请以实际退款到账时间为准。）
         */
    }
}
