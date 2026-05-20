package com.langchain4j.agentic._06_composed_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 候选人工作流接口 - 定义从生活故事到简历生成和优化的完整流程
 */
public interface CandidateWorkflow {
    /**
     * 根据生活故事和职位描述，生成主简历，并通过反馈循环定制简历直到通过评分
     * @param userInfo 用户的生活故事和职业经历
     * @param jobDescription 职位描述
     * @return 优化后的简历
     */
    @Agent("根据生活故事和职位描述，生成主简历，并通过反馈循环定制简历直到通过评分")
    String processCandidate(@V("lifeStory") String userInfo, @V("jobDescription") String jobDescription);
}
