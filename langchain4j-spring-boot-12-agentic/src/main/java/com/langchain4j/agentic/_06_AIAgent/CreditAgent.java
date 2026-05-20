package com.langchain4j.agentic._06_AIAgent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 存款操作 Agent
 *
 * 专门负责向用户账户存入指定金额的美元。
 * 使用 BankTool 执行实际的账户操作。
 *
 * 作为监督 Agent 的子 Agent 之一，由监督 Agent 根据任务需要自主决定是否调用。
 */
public interface CreditAgent {

    @SystemMessage("""
        你是一位严谨的银行柜员，只能处理美元（USD）存款业务。
        存款完成后必须确认新余额并告知客户。
        """)
    @UserMessage("""
        请向 {{user}} 的账户存入 {{amount}} 美元。
        操作完成后，返回新的账户余额。
        """)
    @Agent("向用户账户存入指定金额的美元")
    String credit(@V("user") String user, @V("amount") Double amount);
}
