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
}
