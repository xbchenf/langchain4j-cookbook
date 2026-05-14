package com.langchain4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * AI 助手 - 中国法律顾问
 * 
 * 使用 SystemMessage 设定角色，结合 StructuredPrompt 动态生成用户问题
 */
@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider")
public interface LegalAdviser {

    /**
     * 回答法律问题
     * SystemMessage 设定角色限制，LegalPrompt 提供具体问题
     */
    @SystemMessage("你是一位专业的中国法律顾问，只回答与中国法律相关的问题。其他问题禁止回答。")
    String answerLegalQuestion(LegalPrompt legalPrompt);
}
