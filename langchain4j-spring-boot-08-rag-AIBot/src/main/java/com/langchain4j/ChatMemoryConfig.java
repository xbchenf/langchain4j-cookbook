package com.langchain4j;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为 AI 助手配置对话记忆
 */
@Configuration
public class ChatMemoryConfig {


    /**
     * 配置聊天记忆提供者，为每个用户创建独立的对话记忆
     *
     * 使用场景：
     * - 多用户系统中，每个用户需要保持独立的对话上下文
     * - AI 助手能够记住与特定用户的最近对话历史
     *
     * 工作原理：
     * 1. Spring 框架在需要聊天记忆时调用此 Bean
     * 2. 传入 memoryId（通常是用户ID或会话ID）作为参数
     * 3. Lambda 表达式为每个 memoryId 创建独立的 MessageWindowChatMemory 实例
     * 4. 所有用户的聊天记录存储在内存中（应用重启后会丢失）
     *
     * 配置说明：
     * - id(memoryId): 设置记忆的唯一标识，区分不同用户
     * - chatMemoryStore(new InMemoryChatMemoryStore()): 使用内存存储聊天记录
     *   * 优点：速度快，无需额外配置
     *   * 缺点：应用重启后数据丢失，不适合生产环境
     * - maxMessages(10): 最多保留最近的10条消息，超出自动删除最早的记录
     *   * 防止内存无限增长
     *   * 控制发送给 AI 的上下文长度，节省 Token
     *
     * @return ChatMemoryProvider 聊天记忆提供者，根据 memoryId 返回对应的聊天记忆实例
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        // Lambda 表达式：接收 memoryId 参数，返回为该用户创建的聊天记忆对象
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)                                    // 设置记忆ID，区分不同用户/会话
                .chatMemoryStore(new InMemoryChatMemoryStore())  // 使用内存存储（重启后数据丢失）
                .maxMessages(10)                                 // 窗口大小：只保留最近10条消息
                .build();                                        // 构建并返回聊天记忆实例
    }
}
