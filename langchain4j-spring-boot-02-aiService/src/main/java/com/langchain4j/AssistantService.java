package com.langchain4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * AI 助手服务接口（Spring 自动代理版本）
 *
 * 该接口由 LangChain4j Spring Boot Starter 自动扫描并生成代理实现，
 * 无需编写实现类即可直接注入使用。通过注解声明式配置 AI 行为，
 * 是 Spring 环境下使用 LangChain4j 的最佳实践。
 */
@AiService
interface AssistantService {

    /**
     * 单轮聊天方法
     *
     * 自动附加 @SystemMessage 中定义的系统提示词，引导模型以"AI智能助手"的身份回复。
     * 该方法为无状态调用，每次调用都是独立的对话，不保留历史上下文。
     *
     * @SystemMessage 用于设置系统级提示词，定义 AI 的角色、行为准则和回答风格。
     *                此处设定 AI 为"AI智能助手"，使其回答更专业、友好。
     *
     * @param message 用户输入的聊天消息
     * @return AI 生成的回复文本
     */
    @SystemMessage("你是一个AI智能助手")
    String chat(String message);
}