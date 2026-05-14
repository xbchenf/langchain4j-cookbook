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
public class AssistantConfiguration {


    /*@Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .chatMemoryStore(new InMemoryChatMemoryStore())
                .maxMessages(10)
                .build();
    }*/

    @Bean
    ChatMemoryProvider chatMemoryMysqlProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .chatMemoryStore(new PersistentChatMemoryStore())
                .maxMessages(10)
                .build();
    }
}
