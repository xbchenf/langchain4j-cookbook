import static dev.langchain4j.internal.Utils.randomUUID;
import static dev.langchain4j.store.embedding.chroma.ChromaApiVersion.V2;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.testcontainers.chromadb.ChromaDBContainer;

/**
 * ChromaEmbeddingStore 元数据过滤示例
 *
 * 本示例演示了：
 * 1. 使用 Testcontainers 启动临时的 ChromaDB 容器（用于集成测试）
 * 2. 为文本片段附加元数据（Metadata）
 * 3. 在向量搜索时结合元数据过滤条件，实现多租户/权限隔离
 *
 * 核心场景：RAG 应用中的用户数据隔离
 * - 用户 A 只能搜到自己的文档
 * - 用户 B 只能搜到自己的文档
 * - 即使语义相似，也不会跨用户泄露数据
 */
public class ChromaEmbeddingStoreWithMetadataExample {

    public static void main(String[] args) {

        // ==================== 1. 使用 Testcontainers 启动 ChromaDB ====================

        /**
         * Testcontainers 是一个 Java 库，用于在测试中自动启动 Docker 容器。
         *
         * ChromaDBContainer 会：
         * 1. 拉取 chromadb/chroma:1.1.0 镜像（如果本地没有）
         * 2. 启动一个临时容器，暴露 8000 端口
         * 3. 测试结束后自动销毁容器
         *
         * 这种模式非常适合：
         * - 集成测试（不污染本地/共享数据库）
         * - CI/CD 流水线（无需预装数据库）
         * - 快速原型验证
         *
         * try-with-resources 确保测试结束后容器被自动清理。
         */
        try (ChromaDBContainer chroma = new ChromaDBContainer("chromadb/chroma:1.1.0").withExposedPorts(8000)) {
            chroma.start();

            // ==================== 2. 创建 ChromaEmbeddingStore ====================

            //配置 Chroma 向量存储连接。
            EmbeddingStore<TextSegment> embeddingStore = ChromaEmbeddingStore.builder()
                    .apiVersion(V2)                     //- apiVersion(V2): 使用 Chroma API 版本 2（当前推荐版本）
                    .baseUrl(chroma.getEndpoint())      //- baseUrl: Chroma 服务地址，这里从 Testcontainers 动态获取
                    .collectionName(randomUUID())       //- collectionName: 集合名称（类似数据库表名），使用随机 UUID 避免冲突
                    .logRequests(true)                  //- logRequests/logResponses: 开启请求/响应日志（调试用，生产环境建议关闭）
                    .logResponses(true)
                    .build();

            // ==================== 3. 创建本地嵌入模型 ====================

            /**
             * AllMiniLmL6V2EmbeddingModel：本地轻量级嵌入模型。
             *
             * - 输出 384 维向量
             * - 纯本地推理，无需外部 API
             * - 适合测试和中小型生产场景
             */
            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            // ==================== 4. 添加带元数据的文档 ====================

            /**
             * 为用户 1 添加文档。
             *
             * Metadata.metadata("userId", "1") 创建了一个键值对元数据，
             * 将 "userId=1" 与文本片段关联存储。
             *
             * 元数据的作用：
             * - 搜索过滤：只返回特定 userId 的文档
             * - 数据隔离：实现多租户隔离
             * - 业务标签：分类、时间戳、来源等
             */
            TextSegment segment1 = TextSegment.from("I like football.", Metadata.metadata("userId", "1"));
            Embedding embedding1 = embeddingModel.embed(segment1).content();
            embeddingStore.add(embedding1, segment1);

            /**
             * 为用户 2 添加文档。
             * 注意：语义上 "basketball" 和 "football" 都是运动，
             * 如果不加过滤，搜索"sport"时两者都可能被召回。
             */
            TextSegment segment2 = TextSegment.from("I like basketball.", Metadata.metadata("userId", "2"));
            Embedding embedding2 = embeddingModel.embed(segment2).content();
            embeddingStore.add(embedding2, segment2);

            // ==================== 5. 构建查询向量 ====================

            /**
             * 将查询问题 "What is your favourite sport?" 转换为向量。
             * 这个查询向量将用于与存储的所有文档向量进行相似度比较。
             */
            Embedding queryEmbedding = embeddingModel.embed("What is your favourite sport?").content();

            // ==================== 6. 为用户 1 执行带过滤的搜索 ====================

            /**
             * 构建元数据过滤条件：只匹配 userId = "1" 的文档。
             *
             * metadataKey("userId") 指定要过滤的元数据字段
             * .isEqualTo("1") 指定匹配值
             *
             * 这相当于 SQL 中的：WHERE userId = '1'
             *
             * 在底层，Chroma 会：
             * 1. 先用元数据过滤缩小候选集
             * 2. 再在候选集中做向量相似度搜索
             */
            Filter onlyForUser1 = metadataKey("userId").isEqualTo("1");

            /**
             * 构建搜索请求。
             *
             * .queryEmbedding(queryEmbedding): 查询向量（必须）
             * .filter(onlyForUser1): 元数据过滤条件（可选，但这里是核心演示点）
             *
             * 注意：这里没有设置 maxResults，默认返回 Top-3。
             */
            EmbeddingSearchRequest embeddingSearchRequest1 = EmbeddingSearchRequest
                    .builder()
                    .queryEmbedding(queryEmbedding)
                    .filter(onlyForUser1)
                    .build();

            /**
             * 执行搜索并获取结果。
             *
             * 由于过滤条件限制，只会从 userId=1 的文档中找最相似的。
             * 预期结果："I like football."（score 约 0.8+）
             */
            EmbeddingSearchResult<TextSegment> embeddingSearchResult1 = embeddingStore.search(embeddingSearchRequest1);
            EmbeddingMatch<TextSegment> embeddingMatch1 = embeddingSearchResult1.matches().get(0);

            // 输出相似度分数和匹配文本
            System.out.println(embeddingMatch1.score());        // 例如：0.814...
            System.out.println(embeddingMatch1.embedded().text()); // I like football.

            // ==================== 7. 为用户 2 执行带过滤的搜索 ====================

            /**
             * 同样的查询向量，但过滤条件改为 userId = "2"。
             *
             * 这演示了：相同的语义查询，不同用户看到不同的结果。
             * 这是 SaaS 多租户 RAG 应用的基础隔离机制。
             */
            Filter onlyForUser2 = metadataKey("userId").isEqualTo("2");

            EmbeddingSearchRequest embeddingSearchRequest2 = EmbeddingSearchRequest
                    .builder()
                    .queryEmbedding(queryEmbedding)
                    .filter(onlyForUser2)
                    .build();

            /**
             * 预期结果："I like basketball."（score 约 0.8+）
             * 不会返回 userId=1 的 football 文档，即使语义也很相关。
             */
            EmbeddingSearchResult<TextSegment> embeddingSearchResult2 = embeddingStore.search(embeddingSearchRequest2);
            EmbeddingMatch<TextSegment> embeddingMatch2 = embeddingSearchResult2.matches().get(0);

            System.out.println(embeddingMatch2.score());
            System.out.println(embeddingMatch2.embedded().text());

            // ==================== 8. 清理资源 ====================

            /**
             * 停止并销毁 Testcontainers 容器。
             * try-with-resources 会自动调用 chroma.close()，这里显式调用更保险。
             */
            chroma.stop();
        }
    }
}