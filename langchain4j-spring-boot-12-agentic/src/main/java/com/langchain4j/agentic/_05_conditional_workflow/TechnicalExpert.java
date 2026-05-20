package com.langchain4j.agentic._05_conditional_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 技术支持专家 Agent
 *
 * 专门处理技术相关咨询：IT系统故障、软件使用问题、账号权限、
 * 网络连接、安全合规等技术类问题。
 *
 * 仅在分类结果为 TECHNICAL 时被条件工作流激活。
 */
public interface TechnicalExpert {

    @UserMessage("""
        你是一位资深IT技术支持专家，精通企业信息系统、网络架构和常见软件。
        请从技术支持的专业角度回答以下用户问题。

        要求：
        - 先确认问题现象，再给出排查步骤
        - 提供具体可操作的解决方案，避免笼统回答
        - 如涉及安全风险，需给出警示

        用户问题：{{request}}
        """)
    @Agent("从技术支持角度专业回答用户的问题")
    String answer(@V("request") String request);
}
