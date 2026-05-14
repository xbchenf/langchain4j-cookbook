
package com.langchain4j.aiservice;

import com.langchain4j.aioutput.LostRegisterOutput;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * AI 助手服务接口
 *
 * LangChain4j 会通过 @AiService 注解自动生成实现类，
 * 无需手动编写调用 AI 模型的代码。
 */

@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        /*chatMemoryProvider = "chatMemoryProvider",*/
        tools =  {"chatHistoryTools","lostRegisterTools"}
)
@SystemMessage(fromResource = "lostItemRegister.txt")
public interface LostItemRegisterAssistant {

    @UserMessage("当前sessionId:{{sessionId}};用户当前消息：{{userMessage}}")
    LostRegisterOutput lostItemRegister(@V("sessionId") String sessionId, @V("userMessage") String userMessage);
}

