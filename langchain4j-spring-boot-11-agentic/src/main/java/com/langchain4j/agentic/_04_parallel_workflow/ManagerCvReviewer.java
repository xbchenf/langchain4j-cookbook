package com.langchain4j.agentic._04_parallel_workflow;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 经理简历评审器接口 - 从招聘经理角度根据职位描述评审简历
 */
public interface ManagerCvReviewer {

    /**
     * 根据职位描述评审简历，给出反馈和评分
     * @param cv 候选人简历
     * @param jobDescription 职位描述
     * @return 包含评分和反馈的评审结果
     */
    @Agent(name = "managerReviewer", description = "根据职位描述评审简历，给出反馈和评分")
    @SystemMessage("""
            你是以下职位的招聘经理：
            {{jobDescription}}
            你需要评审申请人的简历，并决定从众多申请人中邀请谁参加现场面试。
            你会给每份简历一个评分和反馈（包括优点和缺点）。
            你可以忽略缺少地址和占位符等内容。
            
            重要提示：仅返回有效的 JSON 格式，换行符使用 \\n，不要有任何 Markdown 格式或代码块。
            """)
    @UserMessage("""
            请评审这份简历：{{candidateCv}}
            """)
    CvReview reviewCv(@V("candidateCv") String cv, @V("jobDescription") String jobDescription);
}
