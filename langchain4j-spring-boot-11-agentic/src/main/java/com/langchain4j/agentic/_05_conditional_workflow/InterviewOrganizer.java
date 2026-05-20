package com.langchain4j.agentic._05_conditional_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 面试组织者接口 - 组织候选人的现场面试
 */
public interface InterviewOrganizer {

    /**
     * 组织申请人的现场面试
     * @param candidateContact 候选人联系信息
     * @param jobDescription 职位描述
     * @return 操作结果描述
     */
    @Agent("组织申请人的现场面试")
    @SystemMessage("""
            你通过向所有相关员工发送日历邀请来组织现场会议，
            安排在当前日期一周后的上午进行 3 小时面试。
            这是相关的职位空缺：{{jobDescription}}
            你还向候选人发送一封祝贺邮件，包含面试详细信息
            以及他在来现场之前应该了解的任何事项。
            最后，你将申请状态更新为“已邀请现场面试”。
            """)
    @UserMessage("""
            为此候选人组织一场现场面试（适用外部访客政策）：{{candidateContact}}
            """)
    String organize(@V("candidateContact") String candidateContact, @V("jobDescription") String jobDescription);
}
