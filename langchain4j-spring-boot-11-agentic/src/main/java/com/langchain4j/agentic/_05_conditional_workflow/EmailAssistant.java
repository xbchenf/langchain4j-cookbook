package com.langchain4j.agentic._05_conditional_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 邮件助手接口 - 向未通过评审的候选人发送拒绝邮件
 */
public interface EmailAssistant {

    /**
     * 向未通过首轮评审的候选人发送拒绝邮件，返回已发送邮件 ID，如果无法发送则返回 0
     * @param candidateContact 候选人联系信息
     * @param jobDescription 职位描述
     * @return 已发送邮件的 ID
     */
    @Agent("向未通过的候选人发送拒绝邮件，返回已发送邮件 ID，如果无法发送则返回 0")
    @SystemMessage("""
            你向未通过首轮评审的候选申请人发送一封友好的邮件。
            你还将申请状态更新为“已拒绝”。
            你返回已发送邮件的 ID。
            """)
    @UserMessage("""
            被拒绝的候选人：{{candidateContact}}
            
            申请职位：{{jobDescription}}
            """)
    int send(@V("candidateContact") String candidateContact, @V("jobDescription") String jobDescription);
}
