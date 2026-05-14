import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import utils.AbstractOllamaInfrastructure;

import java.util.concurrent.CompletableFuture;

/**
 * Ollama 流式聊天模型测试
 *
 * 演示如何使用 LangChain4j 连接 Ollama 的流式（Streaming）API，
 * 实现大模型回答的"逐字输出"效果，提升用户体验。
 *
 * 前置条件：
 * - 如果本地已运行 Ollama，设置环境变量 OLLAMA_BASE_URL（如 http://localhost:11434）
 * - 如果未设置，Testcontainers 会自动拉取并启动 Ollama Docker 容器（首次可能需几分钟）
 *
 * 流式 vs 非流式：
 * - 非流式（ChatModel.chat()）：等待模型生成完整回答后一次性返回
 * - 流式（StreamingChatModel.chat()）：模型每生成一个 token 就实时推送，类似 ChatGPT 的打字效果
 */
@Testcontainers
class OllamaStreamingChatModelTest extends AbstractOllamaInfrastructure {

    /**
     * 流式对话示例：逐字接收模型生成的内容。
     *
     * 核心机制：
     * 1. 使用 StreamingChatModel 替代普通 ChatModel
     * 2. 通过 StreamingChatResponseHandler 回调接口处理流式事件
     * 3. 使用 CompletableFuture 实现异步等待，避免阻塞测试线程
     */
    @Test
    void streaming_example() {

        /**
         * 创建 Ollama 流式聊天模型实例。
         *
         * OllamaStreamingChatModel 与 OllamaChatModel 的区别：
         * - OllamaChatModel：同步阻塞，返回完整字符串
         * - OllamaStreamingChatModel：异步流式，通过回调逐 token 接收
         *
         * 配置项与普通模型相同：baseUrl、modelName、temperature 等。
         */
        StreamingChatModel model = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(MODEL_NAME)
                .build();

        /**
         * 用户提示词。
         *
         * 要求写一首 100 字的关于 Java 和 AI 的诗。
         * 流式输出特别适合长文本生成场景（诗歌、文章、代码等），
         * 用户无需等待全部生成，可实时看到内容涌现。
         */
        String userMessage = "Write a 100-word poem about Java and AI";

        /**
         * CompletableFuture 用于在异步流式回调中同步等待最终完成。
         *
         * 为什么需要它？
         * - 流式 API 是异步的，onPartialResponse/onCompleteResponse 在回调线程执行
         * - 测试方法需要等待流式传输完成后再结束
         * - CompletableFuture 提供了优雅的异步-同步桥接机制
         */
        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        /**
         * 发起流式对话请求。
         *
         * 参数：
         * - userMessage: 用户输入的提示词
         * - StreamingChatResponseHandler: 回调处理器，处理三个事件：
         *   1. onPartialResponse: 每次收到新的 token 片段时触发
         *   2. onCompleteResponse: 模型生成完毕，收到最终完整响应时触发
         *   3. onError: 发生异常时触发
         */
        model.chat(userMessage, new StreamingChatResponseHandler() {

            /**
             * 收到部分响应时触发。
             *
             * 调用时机：模型每生成一个 token（或一小段 token）就会触发一次。
             * 特点：可能被调用多次，每次的 partialResponse 是增量内容。
             *
             * 本示例：直接打印到控制台，实现"打字机"效果。
             * 实际应用：可推送到前端 WebSocket、SSE（Server-Sent Events）等。
             *
             * 注意：不要在此做耗时操作，否则会阻塞回调线程，影响流式接收。
             */
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            /**
             * 收到完整响应时触发。
             *
             * 调用时机：模型生成全部完成，所有 token 都已推送完毕。
             * 参数 completeResponse 包含：
             * - aiMessage(): 完整的 AI 消息（包含最终文本）
             * - tokenUsage(): Token 使用量统计
             * - finishReason(): 结束原因（如 STOP、LENGTH 等）
             *
             * 本示例：将 completeResponse 设置到 CompletableFuture，标记异步操作完成。
             */
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureResponse.complete(completeResponse);
            }

            /**
             * 发生错误时触发。
             *
             * 可能的错误原因：
             * - 网络中断
             * - Ollama 服务异常
             * - 模型加载失败
             * - 请求超时
             *
             * 本示例：将异常设置到 CompletableFuture，使 futureResponse.join() 抛出异常。
             */
            @Override
            public void onError(Throwable error) {
                futureResponse.completeExceptionally(error);
            }
        });

        /**
         * 阻塞等待流式传输完成。
         *
         * futureResponse.join()：
         * - 如果流式传输正常完成，返回 ChatResponse
         * - 如果 onError 被触发，抛出 CompletionException 包装原始异常
         *
         * 在真实应用（如 Spring Boot）中，不需要 join()，
         * 可以直接在 onCompleteResponse 中处理后续逻辑。
         */
        futureResponse.join();
    }
}