
package com.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * AI 助手服务接口 - 演示不同的提示词使用方式
 */
@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider")
public interface Assistant {

    /**
     * 方式1：使用 SystemMessage（推荐）
     * SystemMessage 只在首次对话时发送一次，节省 Token
     * 模版中 {{current_date}} 是 LangChain4j 内置变量
     */
    @SystemMessage(fromResource = "systemMessage.txt")
    String chat(@MemoryId int memoryId, @UserMessage String userMessage);

    /**
     * 方式2：使用 UserMessage 模板
     * 每轮对话都会拼接固定文本，会重复发送背景信息
     */
    @UserMessage("你是我的好朋友，请使用四川话和我聊天。{{message}}")
    String chat3(@MemoryId int memoryId, @V("message") String userMessage);

    /**
     * 方式3：SystemMessage 使用多个动态参数
     * 模板中的 {{name}}、{{age}}、{{current_date}} 会被自动替换
     * 模版中 {{current_date}} 是 LangChain4j 内置变量
     */
    @SystemMessage(fromResource = "systemMessage2.txt")
    String chat4(@MemoryId int memoryId, @UserMessage String userMessage,
                 @V("name") String name, @V("age") int age);
}