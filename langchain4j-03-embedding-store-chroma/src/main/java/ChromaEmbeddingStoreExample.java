import static dev.langchain4j.internal.Utils.randomUUID;
import static dev.langchain4j.store.embedding.chroma.ChromaApiVersion.V2;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import java.util.List;
import org.testcontainers.chromadb.ChromaDBContainer;

/**
 * Chroma 向量存储示例
 * 演示如何使用 Chroma 作为向量数据库来存储和检索文本嵌入向量。
 * Chroma 是一个开源的向量数据库，专门用于存储和查询嵌入向量。
 */
public class ChromaEmbeddingStoreExample {

    public static void main(String[] args) {
        // 使用 Testcontainers 创建并启动一个临时的 ChromaDB 容器实例（版本 1.1.0）
        // try-with-resources 确保容器在使用完毕后自动关闭和清理资源
        try (ChromaDBContainer chroma = new ChromaDBContainer("chromadb/chroma:1.1.0").withExposedPorts(8000)) {
            chroma.start();

            // 构建 Chroma 向量存储实例，配置 API 版本、连接地址和集合名称等参数
            EmbeddingStore<TextSegment> embeddingStore = ChromaEmbeddingStore.builder()
                .apiVersion(V2)                    // 设置 Chroma API 版本为 V2
                .baseUrl(chroma.getEndpoint())     // 设置 ChromaDB 容器的访问地址
                .collectionName(randomUUID())      // 使用随机 UUID 作为集合名称，确保每次运行使用独立的集合
                .logRequests(true)                 // 启用请求日志记录，便于调试和监控
                .logResponses(true)                // 启用响应日志记录，便于调试和监控
                .build();

            // 创建本地嵌入模型，使用 All-MiniLM-L6-v2 模型进行文本向量化

            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            // 准备第一段文本：关于足球的内容
            TextSegment segment1 = TextSegment.from("I like football.");
            // 将第一段文本转换为向量表示
            Embedding embedding1 = embeddingModel.embed(segment1).content();
            // 将向量及其对应的文本片段存储到 Chroma 向量数据库中
            embeddingStore.add(embedding1, segment1);

            // 准备第二段文本：关于天气的内容
            TextSegment segment2 = TextSegment.from("The weather is good today.");
            // 将第二段文本转换为向量表示
            Embedding embedding2 = embeddingModel.embed(segment2).content();
            // 将第二个向量及其对应的文本片段存储到 Chroma 向量数据库中
            embeddingStore.add(embedding2, segment2);

            // 对查询文本 "What is your favourite sport?" 进行向量化
            Embedding queryEmbedding = embeddingModel.embed("What is your favourite sport?").content();
            // 构建向量搜索请求，设置查询向量和最大返回结果数量
            EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)  // 设置查询向量
                    .maxResults(1)                   // 最多返回 1 个最相似的结果
                    .build();
            // 执行向量相似度搜索，查找与查询向量最匹配的文本片段
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();
            // 获取最佳匹配结果（相似度最高的那个）

            EmbeddingMatch<TextSegment> embeddingMatch = matches.get(0);

            // 输出匹配结果的相似度分数（范围 0-1，越接近 1 表示越相似）
            System.out.println(embeddingMatch.score()); // 0.8144288493114709
            // 输出匹配到的文本内容，预期结果为 "I like football."，因为查询是关于运动的

            System.out.println(embeddingMatch.embedded().text()); // I like football.

            // 停止 ChromaDB 容器（try-with-resources 也会自动清理资源）
            chroma.stop();
        }
    }
}
