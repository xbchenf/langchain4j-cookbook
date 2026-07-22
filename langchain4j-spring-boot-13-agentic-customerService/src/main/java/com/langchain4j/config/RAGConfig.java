package com.langchain4j.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

/**
 * RAG（检索增强生成）配置
 *
 * 应用启动时自动加载 policies/ 目录下的政策文档，
 * 切分、向量化并存入 EmbeddingStore。
 *
 * 生产环境建议：
 * - 替换 InMemoryEmbeddingStore 为 Redis / Milvus / Elasticsearch
 * - 使用 langchain4j-redis 或 langchain4j-milvus 等社区集成
 */
@Configuration
@Slf4j
public class RAGConfig {

    /** 内存向量存储（教学环境，零依赖） */
    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * ContentRetriever Bean
     *
     * KnowledgeTools 通过此 Bean 执行 RAG 检索：
     * 用户问题 → Embedding → 向量相似度搜索 → 返回匹配的文档片段
     */
    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                       EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.7)
                .build();
    }

    /**
     * 应用启动时自动构建 RAG 索引
     *
     * 加载 policies/ 目录下所有 .txt 文件：
     * 文档 → 切分 → 向量化 → 存入 EmbeddingStore
     *
     * 教学环境每次启动全量重建（文档量小，秒级完成）。
     * 生产环境应改为增量索引或定时任务。
     */
    @Bean
    ApplicationRunner initRagIndex(EmbeddingModel embeddingModel,
                                    EmbeddingStore<TextSegment> embeddingStore) {
        return args -> {
            try {
                // 定位 policies 目录
                Path policiesDir = Path.of("src/main/resources/policies");
                if (!policiesDir.toFile().exists()) {
                    log.warn("policies 目录不存在: {}, 跳过 RAG 索引构建", policiesDir.toAbsolutePath());
                    return;
                }

                log.info("开始构建 RAG 索引...");

                // 遍历并加载所有 .txt 文件
                java.io.File[] files = policiesDir.toFile()
                        .listFiles((dir, name) -> name.endsWith(".txt"));
                if (files == null || files.length == 0) {
                    log.warn("policies 目录下无 .txt 文件");
                    return;
                }

                DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);

                for (java.io.File file : files) {
                    // 加载文档
                    Document document = FileSystemDocumentLoader.loadDocument(
                            file.toPath(), new TextDocumentParser());

                    // 切分文档段落
                    List<TextSegment> segments = splitter.split(document);

                    // 向量化并存储
                    Response<List<Embedding>> embeddings = embeddingModel.embedAll(segments);
                    embeddingStore.addAll(embeddings.content(), segments);

                    log.info("  indexed: {} → {} segments", file.getName(), segments.size());
                }

                log.info("RAG 索引构建完成！共加载 {} 个文档", files.length);

            } catch (Exception e) {
                log.error("RAG 索引构建失败", e);
            }
        };
    }
}
