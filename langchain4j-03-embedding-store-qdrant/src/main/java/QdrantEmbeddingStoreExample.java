import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import org.testcontainers.qdrant.QdrantContainer;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static dev.langchain4j.internal.Utils.randomUUID;

/**
 * QdrantEmbeddingStore 示例：使用 Testcontainers 进行集成测试
 *
 * 演示流程：
 * 1. 通过 Testcontainers 启动临时 Qdrant 容器
 * 2. 使用原生 QdrantClient 创建 Collection（指定向量维度和距离算法）
 * 3. 使用 LangChain4j 的 QdrantEmbeddingStore 进行向量存取
 * 4. 执行语义搜索并输出最相似结果
 */
public class QdrantEmbeddingStoreExample {

  // Qdrant 默认 gRPC 端口（REST API 默认是 6333）
  private static int grpcPort = 6334;
  // 随机生成 Collection 名称，避免测试冲突
  private static String collectionName = "langchain4j-" + randomUUID();
  // 使用余弦相似度作为距离度量（与语义搜索场景匹配）
  private static Collections.Distance distance = Collections.Distance.Cosine;
  // 向量维度，必须与 EmbeddingModel 输出维度一致
  // AllMiniLmL6V2EmbeddingModel 输出 384 维
  private static int dimension = 384;

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    // ==================== 1. 启动 Qdrant 容器 ====================

    /**
     * Testcontainers 自动拉取并启动 Qdrant 容器。
     * try-with-resources 确保测试结束后容器自动销毁。
     */
    try (QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:latest")) {
      qdrant.start();

      // ==================== 2. 创建 LangChain4j 的 Qdrant 存储 ====================

      /**
       * QdrantEmbeddingStore 是 LangChain4j 对 Qdrant 的封装。
       *
       * 注意：这里只是配置连接信息，此时 Collection 可能还不存在。
       * 因此下一步需要用原生 QdrantClient 显式创建 Collection。
       */
      EmbeddingStore<TextSegment> embeddingStore =
              QdrantEmbeddingStore.builder()
                      .host(qdrant.getHost())
                      .port(qdrant.getMappedPort(grpcPort))
                      .collectionName(collectionName)
                      .build();

      // ==================== 3. 使用原生 Client 创建 Collection ====================

      /**
       * 创建原生 Qdrant gRPC 客户端。
       * 参数：host, port, useTls（false 表示不使用 TLS）
       *
       * ⚠️ 问题修复点：QdrantClient 实现了 Closeable，需要关闭以释放连接资源。
       * 建议改为 try-with-resources 或手动调用 client.close()。
       */
      QdrantClient client =
              new QdrantClient(
                      QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getMappedPort(grpcPort), false)
                              .build());

      /**
       * 异步创建 Collection，并通过 .get() 阻塞等待完成。
       *
       * VectorParams 配置：
       * - size: 向量维度（384），必须与 EmbeddingModel 一致
       * - distance: 距离算法（Cosine），影响相似度计算方式
       *
       * 如果维度不匹配，后续 add 或 search 会报错。
       */
      client.createCollectionAsync(
                      collectionName,
                      Collections.VectorParams.newBuilder()
                              .setDistance(distance)
                              .setSize(dimension)
                              .build())
              .get();

      // ==================== 4. 准备嵌入模型 ====================

      /**
       * AllMiniLmL6V2EmbeddingModel：本地轻量级模型，输出 384 维向量。
       * 与上面创建的 Collection 维度（384）和距离算法（Cosine）匹配。
       */
      EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

      // ==================== 5. 添加文档到 Qdrant ====================

      // 文档 1：关于法国旅行
      TextSegment segment1 = TextSegment.from("I've been to France twice.");
      Embedding embedding1 = embeddingModel.embed(segment1).content();
      embeddingStore.add(embedding1, segment1);

      // 文档 2：关于印度首都
      TextSegment segment2 = TextSegment.from("New Delhi is the capital of India.");
      Embedding embedding2 = embeddingModel.embed(segment2).content();
      embeddingStore.add(embedding2, segment2);

      // ==================== 6. 语义搜索 ====================

      /**
       * 查询："Did you ever travel abroad?"（你出过国吗？）
       * 语义上与 "I've been to France twice." 更相关，
       * 预期会召回该文档而非关于印度首都的文档。
       */
      Embedding queryEmbedding = embeddingModel.embed("Did you ever travel abroad?").content();

      EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
              .queryEmbedding(queryEmbedding)
              .maxResults(1)  // 只返回最相似的 1 条
              .build();

      List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();

      // ==================== 7. 输出结果 ====================

      if (!matches.isEmpty()) {
        EmbeddingMatch<TextSegment> embeddingMatch = matches.get(0);
        System.out.println(embeddingMatch.score());           // 预期：0.6~0.8+
        System.out.println(embeddingMatch.embedded().text()); // 预期：I've been to France twice.
      }

      client.close();//必须关闭 QdrantClient，否则 gRPC 连接池泄漏。

    } // QdrantContainer 自动停止并销毁
  }
}