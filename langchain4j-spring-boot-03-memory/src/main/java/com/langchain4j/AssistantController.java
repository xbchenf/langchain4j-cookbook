package com.langchain4j;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private OpenAiChatModel openAiChatModel;
    /**
     * 构造函数注入 AI 服务
     *
     * @param assistant 普通 AI 助手，用于阻塞式对话
     */
    public AssistantController(Assistant assistant) {
        this.assistant = assistant;


    }

    /**
     * 记忆机制测试1 ：没有配置记忆
     */
    @GetMapping("/testMemory1")
    public void testMemory1() {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(openAiChatModel)
              //  .chatMemory(chatMemory)    // 设置聊天记忆，使助手能够记住对话历史
                .build();
        String answer1=assistant.chat("我叫小白");
        System.out.println(answer1);
        String answer2=assistant.chat("你知道我谁么？");
        System.out.println(answer2);
    }
    /**
     * 记忆机制测试2：手动配置记忆机制
     */
    @GetMapping("/testMemory2")
    public void testMemory2() {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(openAiChatModel)
                .chatMemory(chatMemory)    // 设置聊天记忆，使助手能够记住对话历史
                .build();
        String answer1=assistant.chat("我叫小白");
        System.out.println(answer1);
        String answer2=assistant.chat("你知道我谁么？");
        System.out.println(answer2);
    }
    /**
     * 记忆机制测试3 ：使用@AiService注解中指定的聊天记忆机制   ---推荐使用
     */
    @GetMapping("/assistant")
    public String assistant(@RequestParam(value = "message", defaultValue = "你是谁？") String message) {
        return assistant.chat(message);
    }


}
