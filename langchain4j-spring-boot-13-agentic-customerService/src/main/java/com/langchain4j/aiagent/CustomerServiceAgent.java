package com.langchain4j.aiagent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * 电商售后智能客服 Agent
 *
 * 这是整个系统唯一的 @AiService 入口。
 *
 * 设计要点：
 * 1. 一个接口注册所有 Tool，LLM 自主决策调用哪个工具 —— 不需要 Java 路由代码
 * 2. @MemoryId + ChatMemoryProvider 自动管理多轮对话上下文
 * 3. Flux<String> 返回类型实现真流式逐 token 输出（LangChain4j 自动使用 streamingChatModel）
 * 4. 工具按能力类型分两组：transactionTools（操作型）和 knowledgeTools（知识型）
 *
 * Flux<String> 模式来自 cookbook 03-streaming 示例，
 * LangChain4j 检测到 Flux 返回类型后自动使用 streamingChatModel 逐 token 发射。
 */
@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"transactionTools", "knowledgeTools"})
@SystemMessage(fromResource = "system-prompts/customer-service-agent.txt")
public interface CustomerServiceAgent {

    /**
     * 处理用户消息，返回流式 Flux<String>
     *
     * LangChain4j 自动检测 Flux 返回类型，使用 streamingChatModel
     * 逐 token 发射字符串。无需手动桥接 TokenStream。
     *
     * @param userId  用户ID（@MemoryId 自动注入到 ChatMemoryProvider）
     * @param message 用户输入消息（@UserMessage 自动注入到 LLM 请求）
     * @return Flux<String> 流式输出，每次发射一个 token，前端可逐字显示
     */
    Flux<String> chat(@MemoryId String userId, @UserMessage String message);
}
