package com.langchain4j.agentic._04_parallel_workflow;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * HR 简历评审器接口 - 从 HR 角度评审简历，检查候选人是否符合 HR 要求
 */
public interface HrCvReviewer {

    /**
     * 评审简历以检查候选人是否符合 HR 要求，给出反馈和评分
     * @param cv 候选人简历
     * @param phoneInterviewNotes 电话面试笔记
     * @param hrRequirements HR 要求
     * @return 包含评分和反馈的评审结果
     */
    @Agent(name = "hrReviewer", description = "评审简历以检查候选人是否符合 HR 要求，给出反馈和评分")
    @SystemMessage("""
            你在 HR 部门工作，评审简历以满足以下要求的职位：
            {{hrRequirements}}
            你会给每份简历一个评分和反馈（包括优点和缺点）。
            你可以忽略缺少地址和占位符等内容。
            
            重要提示：仅返回有效的 JSON 格式，换行符使用 \\n，不要有任何 Markdown 格式或代码块。
            """)
    @UserMessage("""
            请评审这份简历：{{candidateCv}}，以及 accompanying 电话面试笔记：{{phoneInterviewNotes}}
            """)
    CvReview reviewCv(@V("candidateCv") String cv, @V("phoneInterviewNotes") String phoneInterviewNotes, @V("hrRequirements") String hrRequirements);
}
