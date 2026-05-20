package com.langchain4j.agentic._06_composed_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 简历改进循环接口 - 定义通过迭代定制和评审来改进简历的流程
 */
public interface CvImprovementLoop {
    /**
     * 通过迭代定制和评审来改进简历，直到通过评分
     * @param cv 当前简历
     * @param jobDescription 职位描述
     * @return 改进后的简历
     */
    @Agent("通过迭代定制和评审来改进简历，直到通过评分")
    String improveCv(@V("cv") String cv, @V("jobDescription") String jobDescription);
}
