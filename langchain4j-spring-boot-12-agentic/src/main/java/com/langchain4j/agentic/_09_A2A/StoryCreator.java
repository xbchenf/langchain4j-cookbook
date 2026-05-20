package com.langchain4j.agentic._09_A2A;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 本地创意写作 Agent（AI Agent）
 *
 * 功能与远程 A2A 服务器上的 A2ACreativeWriter 完全相同。
 * 在开发和测试阶段，作为 A2A 远程 Agent 的本地替代方案使用。
 *
 * ## 本地 vs A2A 远程对比
 *
 *   ┌──────────┬─────────────────┬────────────────────┐
 *   │          │  本地 Agent     │  A2A 远程 Agent    │
 *   ├──────────┼─────────────────┼────────────────────┤
 *   │ 实现     │ interface+LLM   │ A2A 客户端代理     │
 *   │ 位置     │ 当前 JVM        │ 远程服务器         │
 *   │ 通信     │ 直接调用        │ A2A 协议(gRPC/HTTP)│
 *   │ 适用     │ 开发/测试/简单  │ 生产/跨语言/分布式 │
 *   │ 语言限制 │ Java only       │ 无限制             │
 *   └──────────┴─────────────────┴────────────────────┘
 *
 * 当 A2A 服务器就绪后，只需将本 Agent 替换为 A2ACreativeWriter 即可。
 */
public interface StoryCreator {

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
    @Agent("根据给定主题，创作一个富有想象力的短篇故事")
    String writeStory(@V("topic") String topic);
}
