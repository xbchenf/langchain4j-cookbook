package com.langchain4j;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.output.Response;

import static java.time.Duration.ofSeconds;

/**
 * HuggingFace 嵌入模型示例
 *
 * 本示例演示如何使用 HuggingFace Inference API 进行文本嵌入（向量化）。
 * HuggingFace 提供了大量预训练的嵌入模型，可通过其云端 API 直接调用。
 *
 * 使用步骤：
 * 1. 在 https://huggingface.co/ 注册账号
 * 2. 在 Settings → Access Tokens 中创建 API Key
 * 3. 设置环境变量 HF_API_KEY
 *
 * 注意：
 * - 免费账户有速率限制，生产环境建议升级或使用本地模型
 * - waitForModel(true) 会让请求等待模型加载（冷启动可能需要较长时间）
 */
public class T09_HuggingFaceEmbeddingModelExample {

    public static void main(String[] args) {
        // 从环境变量获取 HuggingFace API Key
        String apiKey = System.getenv("HF_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("错误：未设置 HF_API_KEY 环境变量");
            System.err.println("请在 HuggingFace 官网注册并获取 API Key，然后设置环境变量：");
            System.err.println("Windows: set HF_API_KEY=your_api_key");
            System.err.println("Linux/Mac: export HF_API_KEY=your_api_key");
            return;
        }

        // 创建 HuggingFace 嵌入模型实例
        EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
                .accessToken(apiKey)  // HuggingFace API 访问令牌
                .modelId("sentence-transformers/all-MiniLM-L6-v2")  // 使用的模型 ID（轻量级句子嵌入模型）
                .waitForModel(true)   // 如果模型未加载，等待其加载完成（可能耗时较长）
                .timeout(ofSeconds(60))  // 请求超时时间
                .build();

        // 对文本进行嵌入：将文本转换为向量表示
        String text = "Hello, how are you?";
        Response<Embedding> response = embeddingModel.embed(text);
        
        // 输出嵌入结果：包含向量数据和 Token 使用信息
        System.out.println(response);
        // 示例输出：Response { content = Embedding { vector = [0.123, -0.456, ...], dimension = 384 }, tokenUsage = ... }
    }
}
