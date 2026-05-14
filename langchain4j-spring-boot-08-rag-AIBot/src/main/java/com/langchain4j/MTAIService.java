package com.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools =  "dateCalculatorTools",
        contentRetriever = "contentRetriever"
)
public interface MTAIService {

    @SystemMessage("你是美团外卖客服机器人，专门回答美团外卖相关问题")
    @UserMessage("我是用户：{{message}}")
    String sendMessage(@MemoryId String sessionId, @V("message") String message);
}

