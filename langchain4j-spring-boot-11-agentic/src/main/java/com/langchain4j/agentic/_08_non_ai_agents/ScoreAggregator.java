package com.langchain4j.agentic._08_non_ai_agents;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 非 AI 代理 - 将多个简历评审聚合成一个综合评审
 * 这演示了如何将普通 Java 方法作为一等公民代理在代理工作流中使用，
 * 使它们可以与 AI 驱动的代理互换使用。
 * 非 AI 代理适用于需要确定性操作的场景，如计算、数据转换和聚合，
 * 在这些场景中你希望避免 LLM 的参与。
 */
public class ScoreAggregator {

    /**
     * 聚合 HR/经理/团队评审为一个综合评审
     * @param hr HR 评审结果
     * @param mgr 经理评审结果
     * @param team 团队成员评审结果
     * @return 综合评审结果（包含平均分和合并反馈）
     */
    @Agent(description = "聚合 HR/经理/团队评审为一个综合评审", outputKey = "combinedCvReview")
    public CvReview aggregate(@V("hrReview") CvReview hr,
                              @V("managerReview") CvReview mgr,
                              @V("teamMemberReview") CvReview team) {

        System.out.println("ScoreAggregator 被调用，参数：hrReview: " + hr +
                ", managerReview: " + mgr +
                ", teamMemberReview: " + team);

        // 计算平均分数
        double avgScore = (hr.score + mgr.score + team.score) / 3.0;
        
        // 合并所有反馈
        String combinedFeedback = String.join("\n\n",
                "HR 评审：" + hr.feedback,
                "经理评审：" + mgr.feedback,
                "团队成员评审：" + team.feedback
        );
        
        return new CvReview(avgScore, combinedFeedback);
    }
}

