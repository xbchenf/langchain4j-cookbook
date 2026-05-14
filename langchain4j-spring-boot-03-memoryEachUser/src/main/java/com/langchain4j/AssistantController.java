/*
package com.langchain4j.aiservice;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

*/
/**
 * AI 助手控制器
 * 演示如何使用 LangChain4j 的服务调用可视化监控
 *//*

@RestController
public class AssistantController {

    private final Assistant assistant;

    @Autowired
    private OpenAiChatModel openAiChatModel;

    */
/**
     * @param assistant 普通 AI 助手，用于阻塞式对话
     *//*

    public AssistantController(Assistant assistant) {
        this.assistant = assistant;
    }

    @GetMapping("/assistant")
    public String assistant(@RequestParam(value = "message", defaultValue = "你是谁？") String message) {
        return assistant.chat(message);
    }


}
*/
