package com.langchain4j.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天记忆配置
 *
 * 为每个用户（userId）创建独立的 ChatMemory 实例，
 * 让 AI Agent 在多轮对话中自动记住上下文。
 *
 * 生产环境建议：
 * - 替换 InMemoryChatMemoryStore 为数据库持久化方案
 * - 参考 langchain4j-spring-boot-04-inMysqlStore 的 ChatMemoryStore 实现
 */
@Configuration
public class ChatMemoryConfig {

    /** 线程安全的 ChatMemory 注册表，用于查询/清除指定用户的记忆 */
    private final ConcurrentHashMap<Object, ChatMemory> memoryRegistry = new ConcurrentHashMap<>();

    /**
     * ChatMemoryProvider Bean
     *
     * LangChain4j 通过 @MemoryId 注解传入 userId，
     * 调用此 Provider 获取对应用户的 ChatMemory。
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> memoryRegistry.computeIfAbsent(memoryId, id ->
                MessageWindowChatMemory.builder()
                        .id(id)
                        .chatMemoryStore(new InMemoryChatMemoryStore())
                        .maxMessages(20)  // 保留最近 20 条消息，平衡上下文和 token 成本
                        .build()
        );
    }

    /** 查询用户聊天消息（用于前端展示历史记录） */
    public List<ChatMessage> getMessages(Object memoryId) {
        ChatMemory memory = memoryRegistry.get(memoryId);
        return memory != null ? memory.messages() : List.of();
    }

    /** 清除用户聊天记忆 */
    public void clear(Object memoryId) {
        ChatMemory memory = memoryRegistry.get(memoryId);
        if (memory != null) {
            memory.clear();
        }
    }
}
