package com.langchain4j;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/**
 * 为每个用户提供独立记忆的 AI Service 示例
 * 
 * 本示例演示了如何使用 @MemoryId 注解为不同用户维护独立的对话记忆。
 * 在多用户场景下，每个用户的对话历史互不干扰，实现个性化的对话体验。
 * 
 * 核心概念：
 * - @MemoryId：标识用户 ID，LangChain4j 会为每个 ID 创建独立的 ChatMemory
 * - chatMemoryProvider：记忆提供者，根据 memoryId 动态创建或获取对应的记忆实例
 * - 记忆隔离：不同用户的对话历史完全独立，互不影响
 */
public class ServiceWithMemoryForEachUserExample {

    /**
     * 定义支持多用户的 AI 助手接口
     * 
     * 通过 @MemoryId 和 @UserMessage 注解，LangChain4j 能够识别用户身份并管理各自的对话记忆。
     */
    interface Assistant {

        /**
         * 发送消息并获取 AI 回复
         * @param memoryId 用户唯一标识符，用于隔离不同用户的对话记忆
         * @param userMessage 用户输入的消息内容
         * @return AI 生成的回复内容
         */
        String chat(@MemoryId int memoryId, @UserMessage String userMessage);
    }

    public static void main(String[] args) {

        // 创建 OpenAI 聊天模型实例，使用 LangChain4j 提供的演示 API
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")  // 演示 API 地址
                .modelName("gpt-4o-mini")                          // 使用 GPT-4o-mini 模型
                .apiKey("demo")                                    // 演示 API 密钥
                .build();

        // 构建支持多用户的 AI 助手服务实例
        // 使用 chatMemoryProvider 为每个用户 ID 动态创建独立的聊天记忆
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                // 记忆提供者：为每个 memoryId 创建一个最多保留 10 条消息的滑动窗口记忆
                // LangChain4j 会自动缓存这些记忆实例，相同的 memoryId 会复用同一个记忆对象
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // ===== 用户 1（Klaus）的第一次对话 =====
        // memoryId = 1，系统会为 Klaus 创建独立的记忆实例
        System.out.println(assistant.chat(1, "Hello, my name is Klaus"));
        // 输出：Hi Klaus! How can I assist you today?

        // ===== 用户 2（Francine）的第一次对话 =====
        // memoryId = 2，系统会为 Francine 创建另一个独立的记忆实例
        System.out.println(assistant.chat(2, "Hello, my name is Francine"));
        // 输出：Hello Francine! How can I assist you today?

        // ===== 用户 1（Klaus）的第二次对话：询问姓名 =====
        // 使用 memoryId = 1，系统会检索 Klaus 的记忆，找到之前提到的姓名
        System.out.println(assistant.chat(1, "What is my name?"));
        // 输出：Your name is Klaus.

        // ===== 用户 2（Francine）的第二次对话：询问姓名 =====
        // 使用 memoryId = 2，系统会检索 Francine 的记忆，不会受到 Klaus 的影响
        System.out.println(assistant.chat(2, "What is my name?"));
        // 输出：Your name is Francine.

        // ===== 总结 =====
        // 通过这个示例可以看到：
        // 1. 每个用户（memoryId）拥有独立的对话记忆，互不干扰
        // 2. chatMemoryProvider 会根据 memoryId 自动创建和管理多个记忆实例
        // 3. 适用于多用户聊天机器人、客服系统等需要个性化记忆的场景

        /**
         * 运行结果：
         * Hello, Klaus! How can I assist you today?
         * Hello, Francine! How can I assist you today?
         * Your name is Klaus. How can I help you today, Klaus?
         * Your name is Francine. How can I help you today?
         */
    }
}