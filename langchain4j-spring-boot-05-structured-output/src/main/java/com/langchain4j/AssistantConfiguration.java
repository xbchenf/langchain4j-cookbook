package com.langchain4j;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 助手配置类
 * 
 * 负责配置 LangChain4j AI 服务所需的组件，包括：
 * - 聊天记忆提供者（ChatMemoryProvider）：为每个用户创建独立的对话历史存储
 * 
 * 使用 @Configuration 注解，Spring Boot 启动时会自动扫描并注册 Bean
 */
@Configuration
public class AssistantConfiguration {


    /**
     * 配置聊天记忆提供者，为每个用户创建独立的对话记忆
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .chatMemoryStore(new InMemoryChatMemoryStore())  // 内存存储（重启后数据丢失）
                .maxMessages(10)                                 // 只保留最近10条消息
                .build();
    }
}
