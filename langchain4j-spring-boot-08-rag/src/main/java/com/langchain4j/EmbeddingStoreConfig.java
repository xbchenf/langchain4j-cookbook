package com.langchain4j;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingStoreConfig {

    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 向量数据库pinecone 云数据库
     * 提前创建向量存储空间的账号 https://app.pinecone.io
     * https://app.pinecone.io/organizations/-Os6tnmPXB6mRtNGR0s3/projects/0ccc3441-a8bb-4b91-94d1-a5e983ddac72/indexes/my-index/browser
     * @return
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(){
        //创建向量存储空间
        EmbeddingStore<TextSegment> embeddingStore = PineconeEmbeddingStore.builder()
            .apiKey("pcsk_4jhxGV_RjoXaJLTUAe4ysZRKD1qqKnojVKSG9H5b6rbKwaEYSFto1xFP8RtNsT9XvbokD7")
            .index("my-index")//如果指定的索引l不存在，将创建一个新的索引l
            .nameSpace("test-namespace")//如果指定的名称空间不存在，将创建一个新的名称
            .createIndex(PineconeServerlessIndexConfig.builder()
                .cloud("AWS")//指定索引l部署在AWS云服务上。
                .region("us-east-1")//指定索引i所在的 Aws 区域为 us-east-1。
                .dimension(embeddingModel.dimension()) //指定索引向量维度，该维度与 embeddedModel 生成的向量维度相同。
                .build())
            .build();
        return embeddingStore;
    }

}
