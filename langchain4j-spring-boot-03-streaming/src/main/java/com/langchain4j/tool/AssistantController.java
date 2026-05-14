package com.langchain4j.tool;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

/**
 * AI 助手控制器
 *
 * 演示如何使用 LangChain4j 的高级 API（@AiService）实现两种对话模式：
 * 1. 普通对话：等待完整回答后一次性返回
 * 2. 流式对话：实时逐块返回 AI 生成的内容
 */
@RestController
public class AssistantController {

    private final Assistant assistant;
    private final StreamingAssistant streamingAssistant;

    /**
     * 构造函数注入 AI 服务
     *
     * @param assistantService 普通 AI 助手，用于阻塞式对话
     * @param streamingAssistant 流式 AI 助手，用于实时流式对话
     */
    public AssistantController(Assistant assistantService, StreamingAssistant streamingAssistant) {
        this.assistant = assistantService;
        this.streamingAssistant = streamingAssistant;
    }

    /**
     * 普通对话接口
     *
     * 用户发送消息后，等待 AI 生成完整回答再一次性返回。
     * 适用于短文本或对实时性要求不高的场景。
     *
     * @param message 用户输入的消息，默认为 "你是谁？"
     * @return AI 助手的完整回答
     */
    @GetMapping("/assistant")
    public String assistant(@RequestParam(value = "message", defaultValue = "你是谁？") String message) {
        return assistant.chat(message);
    }

    /**
     * 流式对话接口
     *
     * 使用 Server-Sent Events (SSE) 技术，将 AI 生成的内容逐块推送给前端。
     * 用户可以实时看到回答逐步显示，类似 ChatGPT 的打字机效果。
     * 适用于长文本对话，显著提升用户体验。
     *
     * @param message 用户输入的消息，默认为 "你是谁？"
     * @return 响应式数据流，包含 AI 逐块生成的回答内容
     */
    @GetMapping(value = "/streamingAssistant", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamingAssistant(
            @RequestParam(value = "message", defaultValue = "你是谁？") String message) {
        return streamingAssistant.chat(message);
    }
}
