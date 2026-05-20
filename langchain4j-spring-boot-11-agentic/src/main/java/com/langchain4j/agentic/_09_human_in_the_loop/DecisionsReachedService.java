package com.langchain4j.agentic._09_human_in_the_loop;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 决策判断服务 - 用于判断是否已达成决策
 * 这是一个 AI 服务接口，使用小型本地模型即可，因为任务非常简单
 */
public interface DecisionsReachedService {
    /**
     * 根据交互内容判断是否已达成决策
     * @param proposal 秘书的提议（如会议时间建议）
     * @param candidateAnswer 候选人的回答
     * @return 如果已达成决策返回 true，如果需要进一步讨论返回 false
     */
    @SystemMessage("根据交互内容，如果已达成决策则返回 true，如果需要进一步讨论以找到解决方案则返回 false。")
    @UserMessage("""
            到目前为止的交互：
             秘书：{{proposal}}
             被邀请人：{{candidateAnswer}}
    """)
    boolean isDecisionReached(@V("proposal") String proposal, @V("candidateAnswer") String candidateAnswer);
}

