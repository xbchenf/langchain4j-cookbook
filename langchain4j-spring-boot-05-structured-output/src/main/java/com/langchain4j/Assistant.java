
package com.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * AI 助手服务接口 - 演示不同的结构化提取（将返回的内容进行结构化输出）
 */
@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider")
public interface Assistant {

    @UserMessage("extract a number from {{it}}")
    int extractInt(String text);

    @UserMessage("extract a number from {{it}}")
    Long extractLong(String text);

    @UserMessage("extract information about a person from {{it}}")
    Person extractPerson(String text);

    @UserMessage("{{it}}是否为好评?")
    boolean isPositiveReview(String review);

    @UserMessage("分析{{it}}的评价类型")
    ReviewType classifyReview(String review);

    /**
     * 评价类型枚举
     */
    enum ReviewType {
        POSITIVE,   // 好评
        NEGATIVE,   // 差评
        MIXED,      // 既有优点又有缺点
        NEUTRAL     // 中性/无明确态度
    }


}