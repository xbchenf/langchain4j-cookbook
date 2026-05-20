package com.langchain4j.agentic._06_AIAgent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 货币兑换 Agent
 *
 * 专门负责不同货币之间的汇率转换。
 * 使用 ExchangeTool 查询实时汇率并完成换算。
 * 只返回转换后的数字金额，不附带任何说明文字。
 *
 * 作为监督 Agent 的子 Agent 之一，由监督 Agent 根据任务需要自主决定是否调用。
 */
public interface ExchangeAgent {

    @UserMessage("""
        你是一位外汇兑换操作员。
        请使用汇率查询工具将 {{amount}} {{originalCurrency}} 兑换为 {{targetCurrency}}。
        只返回最终的兑换金额数字，不要附带任何其他文字说明。
        """)
    @Agent("将指定金额从原始货币按汇率转换为目标货币，只返回数字金额")
    Double exchange(
            @V("originalCurrency") String originalCurrency,
            @V("amount") Double amount,
            @V("targetCurrency") String targetCurrency);
}
