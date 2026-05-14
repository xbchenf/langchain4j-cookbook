package com.langchain4j;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * AI Service 与聊天记忆（Chat Memory）集成示例
 * 
 * 本示例演示了如何使用 LangChain4j 的 AiServices 功能，将聊天记忆集成到 AI 服务中。
 * 通过这种方式，AI 助手可以记住对话历史，实现上下文相关的多轮对话。
 * 
 * 核心概念：
 * - ChatMemory：存储和管理对话历史
 * - MessageWindowChatMemory：基于消息数量的滑动窗口记忆，只保留最近的 N 条消息
 * - AiServices：声明式 AI 服务，简化与大模型的交互
 */
public class ServiceWithMemoryExample {

    /**
     * 定义 AI 助手接口
     * 
     * 通过简单的接口定义，LangChain4j 会自动生成实现类，处理与大模型的交互。
     * 方法名和参数会被自动转换为合适的提示词（Prompt）。
     */
    interface Assistant {

        /**
         * 发送消息并获取 AI 回复
         * @param message 用户输入的消息
         * @return AI 生成的回复内容
         */
        String chat(String message);
    }

    public static void main(String[] args) {

        // 创建基于消息数量的滑动窗口聊天记忆
        // 最多保留 10 条消息（包括用户消息和 AI 回复），超出后会自动删除最早的消息
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // 创建 OpenAI 聊天模型实例，使用 LangChain4j 提供的演示 API
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")  // 演示 API 地址
                .modelName("gpt-4o-mini")                          // 使用 GPT-4o-mini 模型
                .apiKey("demo")                                    // 演示 API 密钥
                .build();

        // 构建 AI 助手服务实例
        // 将聊天模型和聊天记忆绑定到 Assistant 接口，LangChain4j 会自动生成代理实现
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)          // 设置底层使用的聊天模型
                .chatMemory(chatMemory)    // 设置聊天记忆，使助手能够记住对话历史
                .build();

        // ===== 第一次对话：用户告知姓名 =====
        // 这条消息会被存储到 chatMemory 中
        String answer = assistant.chat("Hello! My name is Klaus.");
        System.out.println(answer); // 输出类似：Hello Klaus! How can I assist you today?

        // ===== 第二次对话：询问之前的信息 =====
        // 由于启用了 chatMemory，AI 能够记住第一条消息中的姓名信息
        // AI 会从 chatMemory 中检索历史对话，提取出用户的姓名是 "Klaus"
        String answerWithName = assistant.chat("What is my name?");
        System.out.println(answerWithName); // 输出类似：Your name is Klaus.

        // ===== 总结 =====
        // 通过这个示例可以看到：
        // 1. ChatMemory 自动管理对话历史，无需手动维护
        // 2. AI Services 提供了简洁的声明式 API，降低了与大模型交互的复杂度
        // 3. 多轮对话中，AI 能够基于上下文给出连贯的回复

        //Hello, Klaus! How can I assist you today?
        //Your name is Klaus. How can I help you today, Klaus?
    }
}
