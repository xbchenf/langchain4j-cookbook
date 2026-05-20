package com.langchain4j.agentic._02_sequential_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 需求分析师 Agent
 * 负责将用户提出的业务需求转化为结构化的需求分析文档，
 * 作为顺序工作流的第一步，为后续方案设计提供清晰的输入。
 */
public interface RequirementAnalyst {

    @UserMessage("""
        你是一位资深需求分析师。请分析以下业务需求，输出一份简洁的需求分析报告。

        要求：
        1. 提炼核心业务目标（1-2句话）
        2. 列出关键功能点（3-5条，标注优先级 P0/P1/P2）
        3. 说明重要的非功能性需求（性能、安全、可用性等）

        业务需求：{{requirement}}
        """)
    @Agent("分析业务需求并输出结构化需求文档")
    String analyze(@V("requirement") String requirement);
}
