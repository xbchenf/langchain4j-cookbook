package com.langchain4j.agentic._06_composed_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 招聘团队工作流接口 - 定义从简历评审到最终决策的完整流程
 */
public interface HiringTeamWorkflow {
    /**
     * 根据简历、电话面试和职位描述，此代理将邀请或拒绝候选人
     * @param candidateCv 候选人简历
     * @param jobDescription 职位描述
     * @param hrRequirements HR 要求
     * @param phoneInterviewNotes 电话面试笔记
     * @param candidateContact 候选人联系信息
     */
    @Agent("根据简历、电话面试和职位描述，此代理将邀请或拒绝候选人")
    void processApplication(@V("candidateCv") String candidateCv,
                          @V("jobDescription") String jobDescription, 
                          @V("hrRequirements") String hrRequirements, 
                          @V("phoneInterviewNotes") String phoneInterviewNotes, 
                          @V("candidateContact") String candidateContact);
}
