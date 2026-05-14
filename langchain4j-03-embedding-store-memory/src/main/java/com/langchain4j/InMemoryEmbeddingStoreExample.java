package com.langchain4j;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

/**
 * InMemoryEmbeddingStore 示例：基于内存的向量存储与语义搜索
 *
 * 本示例演示了 RAG（检索增强生成）流程中的核心环节：
 * 1. 将文本转换为向量（Embedding）
 * 2. 将向量存入内存存储
 * 3. 通过语义相似度搜索最相关的文本片段
 *
 * InMemoryEmbeddingStore 适用于：
 * - 数据量较小的场景（数据全部驻留 JVM 内存）
 * - 开发测试环境
 * - 不需要持久化的临时搜索
 * - 单机应用，无需外部向量数据库（如 Pinecone、Milvus、PgVector）
 */
public class InMemoryEmbeddingStoreExample {

    public static void main(String[] args) {

        // ==================== 1. 创建内存向量存储 ====================

        /**
         * InMemoryEmbeddingStore：基于内存的向量存储实现。
         *
         * 泛型参数 <TextSegment> 表示存储的原始数据类型，这里使用 TextSegment（文本片段）。
         * 你也可以存储其他类型，如 String、Document 等。
         *
         * 特点：
         * - 数据保存在 HashMap 中，完全在内存里
         * - 搜索时使用余弦相似度（Cosine Similarity）计算向量距离
         * - 线程不安全（高并发需自行加锁或使用外部存储）
         */
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // ==================== 2. 创建本地嵌入模型 ====================

        /**
         * AllMiniLmL6V2EmbeddingModel：本地运行的轻量级嵌入模型。
         *
         * - 基于 sentence-transformers/all-MiniLM-L6-v2
         * - 输出 384 维向量
         * - 纯本地推理，无需网络，零 API 费用
         * - 首次初始化时会加载 ONNX 模型到内存（约 80MB）
         *
         * 注意：这是最简单的创建方式，模型文件已打包在依赖中，无需额外配置。
         */
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // ==================== 3. 添加文档到向量存储 ====================

        /**
         * 处理第一条文本：将文本转换为向量并入库。
         *
         * 流程：
         * 1. TextSegment.from("...") 创建文本片段对象
         * 2. embeddingModel.embed(segment) 将文本编码为向量
         *    .content() 获取 Embedding 对象（包含浮点数组）
         * 3. embeddingStore.add(embedding, segment) 将向量与原文本关联存入存储
         */
        TextSegment segment1 = TextSegment.from("I like football.");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        embeddingStore.add(embedding1, segment1);

        // 处理第二条文本：同理入库
        TextSegment segment2 = TextSegment.from("The weather is good today.");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);

        // ==================== 4. 语义搜索 ====================

        /**
         * 将查询问题转换为向量。
         *
         * 这里查询 "What is your favourite sport?"（你最喜欢的运动是什么？）
         * 模型会将其编码为 384 维向量，用于与存储的文档向量进行相似度比较。
         */
        Embedding queryEmbedding = embeddingModel.embed("What is your favourite sport?").content();

        /**
         * 构建搜索请求。
         *
         * EmbeddingSearchRequest 使用 Builder 模式，支持配置：
         * - queryEmbedding: 查询向量（必填）
         * - maxResults: 返回最相似的结果数量（默认 3）
         * - minScore: 最小相似度阈值（0~1，低于此值的结果被过滤）
         * - filter: 元数据过滤条件（如只搜索特定标签的文档）
         */
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)  // 查询向量
                .maxResults(1)                    // 只返回最相似的 1 条结果
                .build();

        /**
         * 执行搜索。
         *
         * search() 方法内部执行：
         * 1. 计算查询向量与存储中所有向量的余弦相似度
         * 2. 按相似度降序排序
         * 3. 返回前 maxResults 条结果
         *
         * 返回的 EmbeddingSearchResult 包含匹配结果列表和可能的分页信息。
         */
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();

        // 获取最匹配的结果（因为 maxResults=1，列表中只有一条）
        EmbeddingMatch<TextSegment> embeddingMatch = matches.get(0);

        // ==================== 5. 输出搜索结果 ====================

        /**
         * score：相似度分数，范围 0~1。
         *
         * - 1.0 表示完全相同
         * - 0.0 表示完全不同（正交）
         * - 通常 > 0.7 认为相关性较高
         *
         * 这里 "What is your favourite sport?" 与 "I like football." 语义相近，
         * 相似度约为 0.81，说明模型成功理解了语义关联。
         */
        System.out.println(embeddingMatch.score()); // 0.8144288515898701

        /**
         * embedded()：获取关联的原始数据对象。
         *
         * 这里返回之前存入的 TextSegment，可以拿到原始文本内容。
         * 在 RAG 应用中，这段文本通常会作为上下文拼接到 LLM 的 Prompt 中。
         */
        System.out.println(embeddingMatch.embedded().text()); // I like football.

        // ==================== 6. 持久化（可选）====================

        /**
         * InMemoryEmbeddingStore 支持序列化，方便保存和恢复。
         *
         * 方式一：序列化为 JSON 字符串
         * 适合：配置中心存储、网络传输、版本控制
         */
        // String serializedStore = embeddingStore.serializeToJson();
        // InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromJson(serializedStore);

        /**
         * 方式二：序列化到本地文件
         * 适合：应用重启后快速恢复向量索引，避免重新计算 Embedding
         *
         * 注意：如果文档量大，文件也会很大；生产环境建议使用专用向量数据库。
         */
        // String filePath = "/home/me/embedding.store";
        // embeddingStore.serializeToFile(filePath);
        // InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromFile(filePath);
    }
}