package com.langchain4j.agentic._05_conditional_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 智能路由编排 Agent（条件工作流入口）
 *
 * 负责串联整个条件工作流：
 *   1. 先由 CategoryRouter 对用户请求进行分类
 *   2. 再根据分类结果，条件性地路由到对应的专业 Agent 处理
 *
 * 这是条件工作流的对外统一入口，用户只需调用一次即可获得专业回复。
 */
public interface ExpertRouterAgent {

    @UserMessage("""
        你是一位企业智能客服系统。
        用户向你提出了以下问题：{{request}}

        请先调用分类工具判断问题类型，
        然后根据分类结果将问题转交给对应的专业同事处理，
        最终将专业同事的回复直接返回给用户。
        """)
    @Agent("先分类用户请求，再路由给对应的专业Agent处理，最终返回专业回复")
    String ask(@V("request") String request);
}
