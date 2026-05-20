package com.langchain4j.domain;

import dev.langchain4j.model.output.structured.Description;

/**
 * 简历评审结果类 - 存储简历评审的评分和反馈信息
 */
public class CvReview {
    @Description("邀请该候选人参加面试的可能性评分，范围从 0 到 1")
    public double score;

    @Description("对简历的反馈，包括优点、需要改进的地方、缺失的技能、警示信号等")
    public String feedback;

    public CvReview() {} // 反序列化需要无参构造函数，因为存在其他构造函数！

    public CvReview(double score, String feedback) {
        this.score = score;
        this.feedback = feedback;
    }

    @Override
    public String toString() {
        return "\n简历评审: " +
                " - 评分 = " + score +
                "\n- 反馈 = \"" + feedback + "\"\n";
    }
}
