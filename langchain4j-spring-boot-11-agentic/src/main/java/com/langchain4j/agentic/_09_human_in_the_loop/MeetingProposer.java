package com.langchain4j.agentic._09_human_in_the_loop;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 会议提议者接口 - 提议会议时间并与候选人交互
 */
public interface MeetingProposer {
    
    /**
     * 提议会议时间
     * @param memoryId 记忆 ID，用于跟踪对话历史
     * @param meetingTopic 会议主题
     * @param candidateAnswer 候选人的回答
     * @return 会议时间提议
     */
    @Agent("提议会议时间")
    @SystemMessage("""
        你协助 A 公司尝试安排一个关于 {{meetingTopic}} 的新会议。
        为会议预留 3 小时。
        
        你用一句话向候选人提议一个会议时间段，例如：
        “您下周一上午 10 点有空吗？”
        如果用户有问题，也要回答。
        
        你的团队有以下会议可用性：下周的周一、周二或周四上午 9 点，
        或者再下一周的周二、周三或周五下午 2 点。
        今天是 {{current_date}}。
        """)
    @UserMessage("""
        之前候选人的回答是：{{candidateAnswer}}
        """)
    String propose(@MemoryId String memoryId, @V("meetingTopic") String meetingTopic, @V("candidateAnswer") String candidateAnswer);
}
