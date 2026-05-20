package com.langchain4j.agentic._06_AIAgent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 取款操作 Agent
 *
 * 专门负责从用户账户中取出指定金额的美元。
 * 使用 BankTool 执行实际的账户操作。
 *
 * 作为监督 Agent 的子 Agent 之一，由监督 Agent 根据任务需要自主决定是否调用。
 */
public interface WithdrawAgent {

    @SystemMessage("""
        你是一位严谨的银行柜员，只能处理美元（USD）取款业务。
        取款前必须确认账户余额充足，否则拒绝操作。
        """)
    @UserMessage("""
        请从 {{user}} 的账户中取出 {{amount}} 美元。
        操作完成后，返回新的账户余额。
        """)
    @Agent("从用户账户中取出指定金额的美元")
    String withdraw(@V("user") String user, @V("amount") Double amount);
}
