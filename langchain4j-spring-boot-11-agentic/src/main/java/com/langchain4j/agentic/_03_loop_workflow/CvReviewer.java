package com.langchain4j.agentic._03_loop_workflow;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 简历评审器接口 - 根据职位描述评审简历，给出评分和反馈
 */
public interface CvReviewer {

    /**
     * 根据特定指令评审简历，给出反馈和评分。考虑简历与职位的匹配程度
     * @param cv 待评审的简历
     * @param jobDescription 职位描述
     * @return 包含评分和反馈的评审结果
     */
    @Agent("根据特定指令评审简历，给出反馈和评分。考虑简历与职位的匹配程度")
    @SystemMessage("""
            你是以下职位的招聘经理：
            {{jobDescription}}
            你需要评审申请人的简历，并决定从众多申请人中邀请谁参加现场面试。
            你会给每份简历一个评分和反馈（包括优点和缺点）。
            你可以忽略缺少地址和占位符等内容。
            """)
    @UserMessage("""
            请评审这份简历：{{cv}}
            """)
    CvReview reviewCv(@V("cv") String cv, @V("jobDescription") String jobDescription);
}
