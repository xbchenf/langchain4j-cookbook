package com.langchain4j;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.concurrent.CompletableFuture;


/**
 * 演示 LangChain4j 的流式聊天（Streaming Chat）功能。
 * 流式响应允许在模型生成文本的过程中逐步接收内容，提升用户体验并减少等待时间。
 */
public class T05_StreamingExamples {

    public static void main(String[] args) {

        // 1. 构建 OpenAI 流式聊天模型实例
        //    OpenAiStreamingChatModel 专门用于流式输出，不会阻塞等待完整响应
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName("gpt-4o-mini")
                .apiKey("demo")
                .build();

        // 2. 定义发送给模型的提示词
        String prompt = "who are you?";


        // 3. 创建 CompletableFuture 用于异步等待流式响应完成
        //    流式调用是异步非阻塞的，需要一种机制来等待最终完成或异常
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();

        // 4. 发送流式聊天请求，并传入自定义的流式响应处理器
        model.chat(prompt, new StreamingChatResponseHandler() {

            /**
             * 每当模型生成一段新文本时触发（即收到一个数据块/Chunk）
             * 可以在此处实现打字机效果，实时展示给用户
             */
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);  // 逐段打印，不换行，实现连续输出效果
            }

            /**
             * 当模型完成全部文本生成后触发
             * 此时可以获取完整的 ChatResponse 对象（包含元数据、Token 使用量等）
             */
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("\n\nDone streaming");
                futureChatResponse.complete(completeResponse);  // 标记异步任务成功完成
            }

            /**
             * 当流式传输过程中发生异常时触发（如网络中断、API 限流等）
             */
            @Override
            public void onError(Throwable error) {
                futureChatResponse.completeExceptionally(error);  // 将异常传递给 CompletableFuture
            }
        });

        // 5. 阻塞主线程，等待流式响应完全结束（正常完成或抛出异常）
        //    在实际应用中（如 Web 服务），通常不需要 join()，而是由框架管理异步生命周期
        futureChatResponse.join();
    }
}