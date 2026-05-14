package com.langchain4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.time.Duration.ofSeconds;

/**
 * Few-Shot Learning（少样本学习）示例
 * 
 * Few-Shot Learning 是一种提示工程技术，通过在对话历史中提供少量示例，
 * 让模型学习特定的响应模式或行为，从而更好地处理新的类似请求。
 * 
 * 本示例演示了如何使用 Few-Shot Learning 来处理用户反馈：
 * - 通过提供正负反馈的示例，教会模型如何分类和响应用户反馈
 * - 模型会根据示例学习判断新消息是正面还是负面反馈，并给出相应回复
 */
public class T07_FewShotExamples {

    public static void main(String[] args) {

        // 创建流式聊天模型实例，使用 LangChain4j 提供的演示 API
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")  // 演示 API 地址
                .modelName("gpt-4o-mini")                          // 使用 GPT-4o-mini 模型
                .apiKey("demo")                                    // 演示 API 密钥
                .timeout(ofSeconds(100))                           // 设置超时时间为 100 秒
                .build();

        // 创建用于 Few-Shot Learning 的对话历史列表
        // 这个列表将包含示例对话和最终的用户消息
        List<ChatMessage> fewShotHistory = new ArrayList<>();

        // ===== 添加第一个示例：正面反馈 =====
        // 用户表达对更新的喜爱，界面友好、功能强大
        fewShotHistory.add(UserMessage.from(
                "I love the new update! The interface is very user-friendly and the new features are amazing!"));
        // AI 的响应：转发到正面反馈存储，并感谢用户
        fewShotHistory.add(AiMessage.from(
                "Action: forward input to positive feedback storage\nReply: Thank you very much for this great feedback! We have transmitted your message to our product development team who will surely be very happy to hear this. We hope you continue enjoying using our product."));

        // ===== 添加第二个示例：负面反馈（技术问题）=====
        // 用户报告 Android 设备更新后频繁崩溃
        fewShotHistory.add(UserMessage
                .from("I am facing frequent crashes after the new update on my Android device."));
        // AI 的响应：创建工单，道歉并承诺尽快修复
        fewShotHistory.add(AiMessage.from(
                "Action: open new ticket - crash after update Android\nReply: We are so sorry to hear about the issues you are facing. We have reported the problem to our development team and will make sure this issue is addressed as fast as possible. We will send you an email when the fix is done, and we are always at your service for any further assistance you may need."));

        // ===== 添加第三个示例：正面反馈 =====
        // 用户称赞应用使日常任务更轻松
        fewShotHistory.add(UserMessage
                .from("Your app has made my daily tasks so much easier! Kudos to the team!"));
        // AI 的响应：转发到正面反馈存储，并表达感谢
        fewShotHistory.add(AiMessage.from(
                "Action: forward input to positive feedback storage\nReply: Thank you so much for your kind words! We are thrilled to hear that our app is making your daily tasks easier. Your feedback has been shared with our team. We hope you continue to enjoy using our app!"));

        // ===== 添加第四个示例：负面反馈（功能问题）=====
        // 用户报告新功能未按预期工作，导致数据丢失
        fewShotHistory.add(UserMessage
                .from("The new feature is not working as expected. It's causing data loss."));
        // AI 的响应：创建工单，道歉并承诺优先处理
        fewShotHistory.add(AiMessage.from(
                "Action: open new ticket - data loss by new feature\nReply:We apologize for the inconvenience caused. Your feedback is crucial to us, and we have reported this issue to our technical team. They are working on it on priority. We will keep you updated on the progress and notify you once the issue is resolved. Thank you for your patience and support."));

        // ===== 添加真实用户的消息 =====
        // 这是一个新的用户抱怨，模型需要根据前面的示例来判断如何处理
        // 用户抱怨应用速度慢，要求改进（这是负面反馈）
        UserMessage customerComplaint = UserMessage
                .from("How can your app be so slow? Please do something about it!");
        fewShotHistory.add(customerComplaint);

        // 输出用户消息
        System.out.println("[User]: " + customerComplaint.singleText());
        System.out.print("[LLM]: ");

        // 创建 CompletableFuture 用于异步接收模型响应
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();

        // 调用流式聊天模型，传入包含示例的完整对话历史
        // 模型会根据前面的示例学习如何分类和响应用户的抱怨
        model.chat(fewShotHistory, new StreamingChatResponseHandler() {

            /**
             * 当接收到部分响应时调用（流式输出）
             * @param partialResponse 模型返回的部分文本片段
             */
            @Override
            public void onPartialResponse(String partialResponse) {
                // 逐块打印模型的响应，实现流式输出效果
                System.out.print(partialResponse);
            }

            /**
             * 当完整响应完成时调用
             * @param completeResponse 完整的聊天响应对象
             */
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 标记 CompletableFuture 为完成状态
                futureChatResponse.complete(completeResponse);
            }

            /**
             * 当发生错误时调用
             * @param error 异常对象
             */
            @Override
            public void onError(Throwable error) {
                // 标记 CompletableFuture 为异常完成状态
                futureChatResponse.completeExceptionally(error);
            }
        });

        // 阻塞等待直到模型响应完成
        futureChatResponse.join();

        // ===== 后续处理 =====
        // 从响应中提取回复内容发送给用户
        // 根据模型判断执行相应的后端操作（如创建工单或存储正面反馈）

        /*
         * [User]: How can your app be so slow? Please do something about it!
         * [LLM]: Action: open new ticket - app performance issue
         * Reply: We apologize for the performance issues you're experiencing with the app. Your feedback is important, and we've forwarded your concerns to our engineering team for investigation. They will look into optimizing the app for better speed. Thank you for your understanding, and we appreciate your patience as we work to improve the experience.
         */
    }
}
