package com.langchain4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多模型聊天控制器
 *
 * 提供三个端点，分别调用不同的 AI 模型：
 * - /model1: DashScope (通义千问)
 * - /model2: Ollama (本地模型)
 * - /model3: OpenAI
 */
@RestController
public class ChatModelController {

    @Autowired
    private DashScopeAssistant dashScopeAssistant;

    @Autowired
    private OllamaAssistant ollamaAssistant;

    @Autowired
    private OpenAiAssistant openAiAssistant;

    @GetMapping("/model1")
    public String model1(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return dashScopeAssistant.chat(message);
    }

    @GetMapping("/model2")
    public String model2(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return ollamaAssistant.chat(message);
    }

    @GetMapping("/model3")
    public String model3(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return openAiAssistant.chat(message);
    }
}
