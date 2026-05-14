package com.langchain4j;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.store.embedding.CosineSimilarity;

import java.io.IOException;

/**
 * 进程内嵌入模型示例
 *
 * 本示例演示如何在 Java 进程中本地运行嵌入模型，无需调用外部 API。
 * 优势：
 * - 完全离线运行，无需网络连接
 * - 无 API 调用限制和费用
 * - 数据隐私性好，文本不会发送到外部服务
 * - 响应速度快，无网络延迟
 */
public class T09_InProcessEmbeddingModelExamples {

    /**
     * 预打包的进程内嵌入模型示例
     *
     * 使用 LangChain4j 提供的预打包模型（all-MiniLM-L6-v2），
     * 只需添加 Maven/Gradle 依赖即可直接使用，无需手动下载模型文件。
     */
    static class Pre_Packaged_In_Process_Embedding_Model_Example {

        public static void main(String[] args) throws IOException {

            String text = "Let's demonstrate that embedding can be done within a Java process and entirely offline.";

            // 需要 "langchain4j-embeddings-all-minilm-l6-v2" Maven/Gradle 依赖（见 pom.xml）
            // AllMiniLmL6V2EmbeddingModel 是一个轻量级的句子嵌入模型，适合快速原型开发
            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            // 在本地进程中执行嵌入：将文本转换为向量
            Embedding inProcessEmbedding = embeddingModel.embed(text).content();
            System.out.println(inProcessEmbedding);

            // 取消注释以与 HuggingFace API 生成的嵌入进行比较
            // EmbeddingModel huggingFaceEmbeddingModel = HuggingFaceEmbeddingModel.builder()
            //        .accessToken(System.getenv("HF_API_KEY"))
            //        .modelId("sentence-transformers/all-MiniLM-L6-v2")
            //        .build();

            //Embedding huggingFaceEmbedding = huggingFaceEmbeddingModel.embed(text).content();

            //System.out.println(CosineSimilarity.between(inProcessEmbedding, huggingFaceEmbedding));
            // 1.000000001963221 <- 这表明离线进程内 all-MiniLM-L6-v2 模型生成的嵌入
            // 与使用 HuggingFace API 生成的嵌入几乎完全相同
        }
    }

    /**
     * 自定义进程内嵌入模型示例
     *
     * 演示如何使用任意的 ONNX 格式嵌入模型。
     * 可以从 HuggingFace 下载模型文件，或将自己训练的模型转换为 ONNX 格式。
     */
    static class Custom_In_Process_Embedding_Model_Example {

        public static void main(String[] args) throws IOException {

            // 可以使用 HuggingFace 上的许多嵌入模型
            // Xenova 仓库（https://huggingface.co/Xenova）提供了大量转换为 ONNX 格式的流行模型

            // 以 multilingual-e5-large 多语言模型为例：
            // 1. 访问 https://huggingface.co/Xenova/multilingual-e5-large
            // 2. 进入 "Files and versions"：https://huggingface.co/Xenova/multilingual-e5-large/tree/main
            // 3. 下载 "tokenizer.json"：https://huggingface.co/Xenova/multilingual-e5-large/resolve/main/tokenizer.json?download=true
            // 4. 进入 "onnx" 目录：https://huggingface.co/Xenova/multilingual-e5-large/tree/main/onnx
            // 5. 下载 "model_quantized.onnx"：https://huggingface.co/Xenova/multilingual-e5-large/resolve/main/onnx/model_quantized.onnx?download=true
            // 6. 访问原始模型仓库：https://huggingface.co/intfloat/multilingual-e5-large
            // 7. 进入 "1_Pooling" 目录：https://huggingface.co/intfloat/multilingual-e5-large/tree/main/1_Pooling
            // 8. 查看 "config.json"：https://huggingface.co/intfloat/multilingual-e5-large/blob/main/1_Pooling/config.json
            //    注意 "pooling_mode_mean_tokens": true，这意味着我们需要使用 PoolingMode.MEAN

            // 也可以通过以下指南将其他模型转换为 ONNX 格式：
            // https://huggingface.co/docs/optimum/exporters/onnx/usage_guides/export_a_model

            // 需要 "langchain4j-embeddings" Maven/Gradle 依赖（见 pom.xml）
            // 创建自定义 ONNX 嵌入模型实例
            EmbeddingModel custom = new OnnxEmbeddingModel(
                    "C:\\dev\\repo\\langchain4j-embeddings\\langchain4j-embeddings-all-minilm-l6-v2\\target\\classes\\ololo\\all-minilm-l6-v2.onnx",  // ONNX 模型文件路径
                    "C:\\dev\\repo\\langchain4j-embeddings\\langchain4j-embeddings-all-minilm-l6-v2\\target\\classes\\all-minilm-l6-v2-tokenizer.json",  // Tokenizer 文件路径
                    PoolingMode.MEAN  // 池化模式：MEAN（平均池化）、MAX（最大池化）、CLS（使用 [CLS] token）
            );

            // 创建预打包的模型实例用于对比
            AllMiniLmL6V2EmbeddingModel packaged = new AllMiniLmL6V2EmbeddingModel();

            String englishText = "Hello, how are you doing?";

            // 分别使用自定义模型和预打包模型生成嵌入
            Embedding customEmbedding = custom.embed(englishText).content();
            Embedding packagedEmbedding = packaged.embed(englishText).content();

            // 计算两个嵌入之间的余弦相似度（越接近 1.0 表示越相似）
            System.out.println(CosineSimilarity.between(customEmbedding, packagedEmbedding));
            // 预期输出：接近 1.0，说明两个模型生成的嵌入非常相似
        }
    }
}
