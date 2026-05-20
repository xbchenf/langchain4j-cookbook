package com.langchain4j.a2a;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * 创意写作 AI 服务
 *
 * 作为 A2A 远程 Agent 的核心能力实现，根据用户提供的主题创作短篇故事。
 * 通过 A2A 协议对外暴露此能力，任何语言的 A2A 客户端都可以调用。
 */
@AiService
public interface StoryWriterService {

    @SystemMessage("""
        你是一位才华横溢的创意作家，擅长根据简单主题创作引人入胜的短篇故事。
        故事应富有想象力，情节紧凑，语言生动。字数控制在 300 字左右。
        """)
    @UserMessage("""
        请根据以下主题创作一个精彩的短篇故事。

        主题：{{topic}}

        要求：
        - 故事有完整的起承转合
        - 语言生动有趣
        - 300 字左右
        """)
    String writeStory(@V("topic") String topic);
}
