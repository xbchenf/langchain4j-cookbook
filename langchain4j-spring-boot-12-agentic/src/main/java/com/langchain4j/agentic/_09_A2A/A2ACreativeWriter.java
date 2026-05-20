package com.langchain4j.agentic._09_A2A;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * A2A 远程创意写作 Agent 客户端接口（Typed A2A Client）
 *
 * ## A2A 协议简介
 *
 * A2A（Agent-to-Agent）是 Google 提出的开放协议，用于不同 Agent 之间的标准化通信。
 * 通过 A2A，Java Agent 可以直接调用远程的 Python/TypeScript/其他语言 Agent，
 * 实现跨语言、跨平台的 Agent 协作。
 *
 * ## 接口说明
 *
 * 这是一个 A2A 客户端的本地接口映射。远程 A2A 服务器上运行着一个
 * "创意写作 Agent"，负责根据给定的主题生成精彩的故事。
 *
 * ## 使用方式
 *
 * ```java
 * // 通过 AgenticServices 创建 A2A 客户端代理
 * A2ACreativeWriter writer = AgenticServices
 *         .a2aBuilder("http://localhost:11000", A2ACreativeWriter.class)
 *         .outputKey("story")
 *         .build();
 *
 * // 像调用本地 Agent 一样调用远程 Agent
 * String story = writer.writeStory("龙与魔法师");
 * ```
 *
 * ## 远程 A2A 服务器端的 Agent 配置参考
 *
 * 远程服务器需要发布一个 AgentCard，描述此 Agent 的能力：
 *
 * ```java
 * AgentCard card = new AgentCard.Builder()
 *         .name("创意写作助手")
 *         .description("根据主题生成富有想象力的短篇故事")
 *         .skills(List.of(new AgentSkill.Builder()
 *                 .id("creative_writing")
 *                 .name("创意写作")
 *                 .description("根据给定主题创作短篇故事")
 *                 .tags(List.of("写作", "创意", "故事"))
 *                 .examples(List.of("写一个关于龙与魔法师的故事"))
 *                 .build()))
 *         .build();
 * ```
 *
 * @see <a href="https://docs.langchain4j.dev">LangChain4j A2A 集成文档</a>
 */
public interface A2ACreativeWriter {

    @Agent("根据给定主题创作一个富有想象力的短篇故事（部署在远程 A2A 服务器上）")
    String writeStory(@V("topic") String topic);
}
