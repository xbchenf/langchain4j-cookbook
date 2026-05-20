package com.langchain4j.agentic._09_human_in_the_loop;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 招聘决策提议者接口 - 总结招聘决策供最终人工验证
 */
public interface HiringDecisionProposer {
    
    /**
     * 总结招聘决策供最终人工验证
     * @param cvReview 简历评审结果（包含所有参与方的反馈）
     * @return 最多 3 行的招聘原因总结，供人类做出最终决定
     */
    @Agent("总结招聘决策供最终验证")
    @SystemMessage("""
        你根据给定的评审结果，在最多 3 行内总结招聘原因，
        供人类做出是否继续的最终决定。
        """)
    @UserMessage("""
        招聘过程中所有相关方的反馈：{{cvReview}}
        """)
    String propose(@V("cvReview") CvReview cvReview);
}
