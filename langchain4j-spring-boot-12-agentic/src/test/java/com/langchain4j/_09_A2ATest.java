package com.langchain4j;

import com.langchain4j.agentic._09_A2A.A2ACreativeWriter;
import com.langchain4j.agentic._09_A2A.StoryCreator;
import com.langchain4j.agentic._09_A2A.StoryStyleEditor;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
A2A（Agent-to-Agent）协议集成测试类
 *
## A2A 协议简介
 *
A2A 是 Google 提出的开放标准，用于不同 Agent 之间的互操作通信。
通过 A2A，Java Agent 可以无缝调用远程的 Python/TypeScript/其他语言 Agent，
实现真正的跨语言、跨平台分布式 Agent 系统。
 *
## 架构全景
 *
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  本地 Agent 系统 (Java / LangChain4j)                    │
 *   │                                                         │
 *   │  ┌──────────────────────┐    ┌──────────────────────┐   │
 *   │  │ StoryCreator (本地) │ → │ StoryStyleEditor (本地)│   │
 *   │  │ 创作故事初稿         │    │ 润色文风              │   │
 *   │  └──────────────────────┘    └──────────────────────┘   │
 *   │                                                         │
 *   │  ★ 生产环境中，StoryCreator 可替换为 A2A 远程 Agent：    │
 *   │                                                         │
 *   │  ┌──────────────────────┐                               │
 *   │  │ A2ACreativeWriter    │ ← A2A 协议(gRPC/JSON-RPC)    │
 *   │  │ (A2A 客户端代理)     │                               │
 *   │  └──────────┬───────────┘                               │
 *   │             │                                           │
 *   └─────────────┼───────────────────────────────────────────┘
 *                 │ A2A 协议
 *     ┌───────────▼─────────────────────────────────┐
 *     │  A2A 远程服务器 (可以是 Python/TS/Go 等)      │
 *     │                                              │
 *     │  ┌──────────────────────────────────────┐    │
 *     │  │ 创意写作 Agent                        │    │
 *     │  │ - AgentCard 描述能力                  │    │
 *     │  │ - 支持 gRPC + JSON-RPC 传输           │    │
 *     │  └──────────────────────────────────────┘    │
 *     └──────────────────────────────────────────────┘
 *
## A2A 核心概念
 *
AgentCard（Agent 名片）：
 *   远程 Agent 通过 AgentCard 自描述其能力，包括名称、技能、传输协议、
 *   端点 URL 等。客户端通过读取 AgentCard 自动发现 Agent 的能力。
 *
传输协议：
 *   - gRPC（推荐）：高性能二进制协议，适合生产环境
 *   - JSON-RPC over HTTP：兼容性好，适合调试和简单场景
 *
## 本示例说明
 *
由于 A2A 需要远程服务器运行，本测试提供两个版本：
 *   1. 本地版（可运行）：使用 StoryCreator（本地 Agent）→ StoryStyleEditor
 *   2. A2A 版（架构参考）：使用 A2ACreativeWriter（远程）→ StoryStyleEditor
 */
@SpringBootTest
public class _09_A2ATest {

    @Autowired
    private OpenAiChatModel chatModel;

    /**
     * 测试本地版工作流：创作 + 润色
     *
     * 展示了本地 Agent 串联的完整流程，同时也是 A2A 架构的本地模拟。
     * 当 A2A 远程服务器就绪后，只需将 StoryCreator 替换为 A2ACreativeWriter 即可。
     */
    @Test
    public void testLocalStoryWorkflow() {

        // 构建本地创意写作 Agent（生成故事初稿）
        StoryCreator storyCreator = AgenticServices
                .agentBuilder(StoryCreator.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        // 构建本地风格润色 Agent（优化文风）
        StoryStyleEditor styleEditor = AgenticServices
                .agentBuilder(StoryStyleEditor.class)
                .chatModel(chatModel)
                .outputKey("finalStory")
                .build();

        // 构建顺序工作流：先创作 → 再润色
        UntypedAgent storyPipeline = AgenticServices
                .sequenceBuilder()
                .subAgents(storyCreator, styleEditor)
                .outputKey("finalStory")
                .build();

        System.out.println("======================================================");
        System.out.println("【A2A 示例·本地版】创意写作 + 风格润色 顺序工作流");
        System.out.println("======================================================");

        // 用户只需提供一个主题
        Map<String, Object> input = Map.of("topic", "一只会说话的猫在深夜的图书馆里冒险");

        System.out.println("输入主题：" + input.get("topic"));
        System.out.println("------------------------------------------------------");

        String finalStory = (String) storyPipeline.invoke(input);

        System.out.println("\n======================================================");
        System.out.println("【最终交付】润色后的故事");
        System.out.println("======================================================");
        System.out.println(finalStory);
        System.out.println("======================================================");
    }

    /**
     * A2A 远程 Agent 集成示例（架构参考）
     *
     * ★ 注意：此测试需要先启动 A2A 远程服务器才能运行。
     * 如果你没有可用的 A2A 服务器，请运行 testLocalStoryWorkflow() 体验相同的流程。
     *
     * 客户端（Java）无需知道远程 Agent 的实现细节，
     * 只通过 A2A 协议与之通信——实现真正的语言无关 Agent 协作。
     */
    @Test
    public void testA2ARemoteWorkflow() {
        // 步骤 1：创建 A2A 客户端代理（替代本地 StoryCreator）
        // A2A 框架会自动从 http://localhost:11000 获取 AgentCard，
        // 解析其能力描述和技能列表
        A2ACreativeWriter a2aWriter = AgenticServices
                .a2aBuilder("http://localhost:11000", A2ACreativeWriter.class)
                .outputKey("story")
                .build();
        // 步骤 2：构建本地润色 Agent（与本地版完全相同）
        StoryStyleEditor styleEditor = AgenticServices
                .agentBuilder(StoryStyleEditor.class)
                .chatModel(chatModel)
                .outputKey("finalStory")
                .build();

        // 步骤 3：使用监督 Agent 编排远程 + 本地 Agent
        // 监督 Agent 会先调用远程的创意写作 Agent，再调用本地的润色 Agent
        SupervisorAgent storySupervisor = AgenticServices
                .supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(a2aWriter, styleEditor)
                .build();

        // 步骤 4：用自然语言描述需求，监督 Agent 自主编排
        System.out.println("======================================================");
        System.out.println("【A2A 示例·远程】创意写作 + 风格润色 顺序工作流");
        String result = storySupervisor.invoke("请创作并润色一个故事，主题是龙与魔法师");
        System.out.println(result);
        System.out.println("======================================================");
    }
}
