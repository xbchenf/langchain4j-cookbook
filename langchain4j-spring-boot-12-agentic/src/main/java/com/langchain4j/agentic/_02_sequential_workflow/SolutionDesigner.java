package com.langchain4j.agentic._02_sequential_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 方案设计师 Agent
 * 负责基于需求分析文档设计技术实现方案，
 * 作为顺序工作流的第二步，将需求转化为可落地的技术架构。
 */
public interface SolutionDesigner {

    @UserMessage("""
        你是一位技术架构师。请基于以下需求分析，设计一份技术方案。

        要求：
        1. 给出系统整体架构思路（如分层架构、微服务等）
        2. 推荐核心技术栈并简述选型理由
        3. 描述1-2个核心模块的设计要点

        需求分析文档：{{document}}
        """)
    @Agent("基于需求分析设计技术解决方案")
    String design(@V("document") String document);
}
