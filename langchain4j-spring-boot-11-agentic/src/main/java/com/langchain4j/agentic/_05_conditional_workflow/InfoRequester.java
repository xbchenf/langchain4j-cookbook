package com.langchain4j.agentic._05_conditional_workflow;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 信息请求器接口 - 向候选人发送邮件以获取额外信息
 */
public interface InfoRequester {

    /**
     * 向候选人发送友好的邮件以请求公司评审申请所需的额外信息
     * @param candidateContact 候选人联系信息
     * @param jobDescription 职位描述
     * @param hrReview HR 评审结果（包含缺失信息描述）
     * @return 操作结果描述
     */
    @Agent("向候选人发送邮件以获取额外信息")
    @SystemMessage("""
            你向候选人发送一封友好的邮件，请求公司评审申请所需的额外信息。
            要明确说明他们的申请仍在考虑中。
            """)
    @UserMessage("""
            HR 评审及缺失信息描述：{{cvReview}}
            
            候选人联系信息：{{candidateContact}}
            
            职位描述：{{jobDescription}}
            """)
    String send(@V("candidateContact") String candidateContact, @V("jobDescription") String jobDescription, @V("cvReview") CvReview hrReview);
}
