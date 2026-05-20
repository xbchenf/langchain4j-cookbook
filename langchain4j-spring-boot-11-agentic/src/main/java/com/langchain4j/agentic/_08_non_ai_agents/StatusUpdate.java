package com.langchain4j.agentic._08_non_ai_agents;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 非 AI 代理 - 根据综合评审分数更新申请状态
 * 这演示了如何将普通 Java 方法作为一等公民代理在代理工作流中使用，
 * 使它们可以与 AI 驱动的代理互换使用。
 * 此代理用于确定性操作：根据分数阈值更新数据库中的申请状态。
 */
public class StatusUpdate {

    /**
     * 根据综合评审分数更新申请状态
     * @param aggregateCvReview 综合简历评审结果
     */
    @Agent(description = "根据分数更新申请状态")
    public void update(@V("combinedCvReview") CvReview aggregateCvReview) {
        double score = aggregateCvReview.score;
        System.out.println("StatusUpdate 被调用，分数：" + score);

        // 根据分数阈值决定申请状态
        if (score >= 8.0) {
            // 演示用的虚拟数据库更新
            System.out.println("申请状态已更新为：已邀请");
        } else {
            // 演示用的虚拟数据库更新
            System.out.println("申请状态已更新为：已拒绝");
        }
    }
}

