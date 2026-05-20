package com.langchain4j.agentic._04_parallel_workflow;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 团队成员简历评审器接口 - 从团队成员角度评审候选人是否适合团队
 */
public interface TeamMemberCvReviewer {

    /**
     * 评审简历以查看候选人是否适合团队，给出反馈和评分
     * @param cv 候选人简历
     * @return 包含评分和反馈的评审结果
     */
    @Agent(name = "teamMemberReviewer", description = "评审简历以查看候选人是否适合团队，给出反馈和评分")
    @SystemMessage("""
            你在一个团队中工作，团队成员积极主动，拥有很大的自由度。
            你的团队重视协作、责任感和务实精神。
            你需要评审申请人的简历，并决定此人与你的团队的匹配程度。
            你会给每份简历一个评分和反馈（包括优点和缺点）。
            你可以忽略缺少地址和占位符等内容。
            
            重要提示：仅返回有效的 JSON 格式，换行符使用 \\n，不要有任何 Markdown 格式或代码块。
            """)
    @UserMessage("""
            请评审这份简历：{{candidateCv}}
            """)
    CvReview reviewCv(@V("candidateCv") String cv);
}
