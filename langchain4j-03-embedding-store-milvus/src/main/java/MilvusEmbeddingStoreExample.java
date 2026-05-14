import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.testcontainers.milvus.MilvusContainer;

import java.util.List;

/**
 * MilvusEmbeddingStore 示例：使用 Testcontainers 进行集成测试
 *
 * 演示流程：
 * 1. 通过 Testcontainers 启动临时 Milvus 容器
 * 2. 配置 LangChain4j 的 MilvusEmbeddingStore（自动创建 Collection）
 * 3. 将文本转换为向量并写入 Milvus
 * 4. 执行语义相似度搜索
 *
 * Milvus 特点：
 * - 企业级分布式向量数据库，适合大规模数据（十亿级）
 * - 需要显式指定向量维度（dimension），创建 Collection 后不可更改
 * - 架构较重（依赖 Etcd、MinIO、Pulsar 等），Testcontainers 镜像较大
 */
public class MilvusEmbeddingStoreExample {

    public static void main(String[] args) {

        // ==================== 1. 启动 Milvus 容器 ====================

        /**
         * 使用 Testcontainers 启动 Milvus v2.3.1。
         *
         * 注意：
         * - Milvus 镜像体积较大（约 1GB+），首次拉取可能较慢
         * - Milvus 2.x 默认暴露 gRPC（19530）和 REST（9091）端口
         * - Testcontainers 会自动处理端口映射和等待策略
         *
         * try-with-resources 确保测试结束后容器自动销毁。
         */
        try (MilvusContainer milvus = new MilvusContainer("milvusdb/milvus:v2.3.1")) {
            milvus.start();

            // ==================== 2. 创建 MilvusEmbeddingStore ====================

            /**
             * 配置 Milvus 向量存储连接。
             *
             * 参数说明：
             * - uri: Milvus 服务端点，Testcontainers 动态生成
             * - collectionName: Collection 名称（类似数据库表名）
             * - dimension: 向量维度（384），必须与 EmbeddingModel 输出维度一致
             *
             * 内部行为：
             * - 首次 add() 时，如果 Collection 不存在，会自动创建
             * - 自动使用 HNSW 索引和余弦相似度（Cosine）
             * - 创建后 dimension 固定，后续写入的向量维度必须一致
             */
            EmbeddingStore<TextSegment> embeddingStore = MilvusEmbeddingStore.builder()
                    .uri(milvus.getEndpoint())
                    .collectionName("test_collection")
                    .dimension(384)
                    .build();

            // ==================== 3. 创建本地嵌入模型 ====================

            /**
             * AllMiniLmL6V2EmbeddingModel：本地轻量级模型。
             *
             * - 输出 384 维向量
             * - 纯本地推理，无需外部 API
             * - 与 MilvusEmbeddingStore 中配置的 dimension=384 必须匹配
             */
            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            // ==================== 4. 添加文档到 Milvus ====================

            // 文档 1：关于足球
            TextSegment segment1 = TextSegment.from("I like football.");
            Embedding embedding1 = embeddingModel.embed(segment1).content();
            embeddingStore.add(embedding1, segment1);

            // 文档 2：关于天气
            TextSegment segment2 = TextSegment.from("The weather is good today.");
            Embedding embedding2 = embeddingModel.embed(segment2).content();
            embeddingStore.add(embedding2, segment2);

            // ==================== 5. 语义搜索 ====================

            /**
             * 查询："What is your favourite sport?"（你最喜欢的运动是什么？）
             *
             * 语义分析：
             * - "football" 属于运动，与查询语义高度相关
             * - "weather" 与运动无关，相似度应较低
             * 预期召回文档 1："I like football."
             */
            Embedding queryEmbedding = embeddingModel.embed("What is your favourite sport?").content();

            EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(1)  // 只返回最相似的 1 条
                    .build();

            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();

            // ==================== 6. 输出结果 ====================

            /**
             * ⚠️ 潜在问题：matches 可能为空（如 Collection 为空、网络异常等），
             * 直接调用 get(0) 会抛出 IndexOutOfBoundsException。
             *
             * 生产环境建议先判空：
             * if (!matches.isEmpty()) { ... }
             */
            if (!matches.isEmpty()) {
                EmbeddingMatch<TextSegment> embeddingMatch = matches.get(0);

                /**
                 * score: 相似度分数，范围通常为 0~1（Cosine 距离转换）。
                 * 值越大表示语义越相似。
                 * 预期输出约 0.81，说明 "football" 与 "favourite sport" 语义接近。
                 */
                System.out.println(embeddingMatch.score()); // 0.8144287765026093

                // embedded(): 获取关联的原始文本片段
                System.out.println(embeddingMatch.embedded().text()); // I like football.
            } else {
                System.out.println("No matches found.");
            }

        } // MilvusContainer 自动停止并销毁，释放端口和磁盘资源
    }
}