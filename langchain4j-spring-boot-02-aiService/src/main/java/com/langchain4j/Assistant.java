package com.langchain4j;

/**
 * AI 助手基础接口（手动代理版本）
 *
 * 这是一个最简化的 AI 服务接口，仅定义了聊天契约。
 * 需要通过 AiServices.create(Assistant.class, chatModel) 手动创建代理实例。
 *
 * 与 @AiService 注解版本的区别：
 * - 本接口：纯接口定义，无注解支持，需手动创建代理，灵活性低
 * - @AiService 版本：支持 @SystemMessage 等注解，由 Spring 自动管理
 *
 * 适用于非 Spring 环境或需要手动控制代理创建过程的场景。
 */
interface Assistant {

    /**
     * 发送消息并获取 AI 回复
     *
     * 基础的聊天方法，不包含系统提示词、记忆管理等高级功能。
     * 如需这些功能，请使用 {@link AssistantService} 或在创建代理时通过
     * AiServices 构建器链式配置（如 .systemMessageProvider(...)）。
     *
     * @param message 用户输入的聊天消息
     * @return AI 生成的回复文本
     */
    String chat(String message);
}