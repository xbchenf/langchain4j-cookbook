package com.langchain4j;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * 为 AI 助手配置对话记忆
 */
@Configuration
public class AssistantConfiguration {

    /**
     * 创建原型作用域的聊天记忆，保留最近 10 条消息，支持多轮对话上下文
     */
    @Bean
    @Scope(SCOPE_PROTOTYPE)
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
    }
}
