
package com.langchain4j.tool;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * AI 助手服务接口
 *
 * LangChain4j 会通过 @AiService 注解自动生成实现类，
 * 无需手动编写调用 AI 模型的代码。
 */
@AiService
public interface Assistant {

    /**
     * 与 AI 助手进行对话
     *
     * @param userMessage 用户输入的消息
     * @return AI 助手的回复
     */
    @SystemMessage("你是一个AI智能助手")
    String chat(String userMessage);
}