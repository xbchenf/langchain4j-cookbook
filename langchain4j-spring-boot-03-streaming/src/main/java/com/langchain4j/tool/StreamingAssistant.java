package com.langchain4j.tool;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

/**
 * 流式 AI 助手服务接口
 *
 * 提供基于响应式流的对话功能，支持实时逐块返回 AI 生成的内容。
 * 适用于需要提升用户体验的长文本对话场景，类似 ChatGPT 的打字机效果。
 *
 * LangChain4j 会通过 @AiService 注解自动生成实现类，
 * 底层调用 OpenAI 的流式 API，将回答以 Flux 数据流的形式返回。
 */
@AiService
public interface StreamingAssistant {

    /**
     * 流式对话方法
     *
     * 接收用户消息后，通过响应式流（Flux）实时推送 AI 生成的内容片段。
     * 前端可以立即显示每一段内容，无需等待完整回答生成。
     *
     * @param userMessage 用户输入的消息
     * @return 响应式数据流，包含 AI 逐块生成的回答内容
     */
    @SystemMessage("你是一个AI智能助手")
    Flux<String> chat(String userMessage);
}
