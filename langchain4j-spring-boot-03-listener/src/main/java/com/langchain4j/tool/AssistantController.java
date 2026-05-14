package com.langchain4j.tool;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 助手控制器
 *
 * 演示如何使用 LangChain4j 的服务调用可视化监控
 */
@RestController
public class AssistantController {

    private final Assistant assistant;

    /**
     * 构造函数注入 AI 服务
     *
     * @param assistant 普通 AI 助手，用于阻塞式对话
     */
    public AssistantController(Assistant assistant) {
        this.assistant = assistant;
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


}
