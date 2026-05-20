package com.langchain4j;

import com.langchain4j.agentic._06_AIAgent.BankTool;
import com.langchain4j.agentic._06_AIAgent.CreditAgent;
import com.langchain4j.agentic._06_AIAgent.WithdrawAgent;
import com.langchain4j.agentic._07_NonAIAgent.ExchangeOperator;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 非 AI Agent 测试类
 *
 * ## 核心概念
 *
 * 前面所有示例中的 Agent 都是 AI Agent（基于 LLM），
 * 但 langchain4j-agentic 也支持非 AI Agent——即普通的 Java 类/方法。
 *
 * 非 AI Agent 的本质：
 *   - 就是一段普通 Java 代码，不调用任何 LLM
 *   - 通过 @Agent 注解被监督 Agent 识别和调用
 *   - 与 AI Agent 在同一个工作流中无缝混用
 *
 * ## AI Agent vs 非 AI Agent 对比
 *
 *   ┌────────────┬─────────────────┬──────────────────┐
 *   │            │   AI Agent      │   非 AI Agent    │
 *   ├────────────┼─────────────────┼──────────────────┤
 *   │ 实现方式   │ interface + LLM │ class + Java代码 │
 *   │ 适用场景   │ 自然语言理解    │ 确定性操作       │
 *   │ 速度       │ 慢（等待LLM）   │ 快（JVM直接执行）│
 *   │ 成本       │ 有（API调用费） │ 零               │
 *   │ 确定性     │ 可能不同        │ 100%确定         │
 *   │ 典型例子   │ 客服回复、分类  │ 汇率计算、API调用│
 *   └────────────┴─────────────────┴──────────────────┘
 *
 * ## 本示例对比
 *
 *   _06 方案（错误示范）：
 *   ExchangeAgent 是 AI Agent → 每次汇率换算都调用 LLM
 *   "100 EUR 等于多少 USD？" → LLM 回复 "108 USD" → 多余且昂贵
 *
 *   _07 方案（正确示范）：
 *   ExchangeOperator 是非 AI Agent → 纯 Java 计算
 *   exchange("EUR", 100.0, "USD") → Math.round(100*1.08/1.0) = 108.0
 *
 * 原则：能用代码确定性地完成的步骤，就不要让 LLM 来做。
 * 非 AI Agent 越多，你的系统就越快、越准、越省钱。
 */
@SpringBootTest
public class _07_NonAIAgentTest {

    @Autowired
    private OpenAiChatModel chatModel;

    private BankTool bankTool;

    @BeforeEach
    public void setUp() {
        bankTool = new BankTool();
        bankTool.createAccount("张三", 1000.0);
        bankTool.createAccount("李四", 1000.0);
    }

    /**
     * 测试 AI Agent 与非 AI Agent 混合使用
     *
     * 监督 Agent 拥有三个子 Agent：
     *   - WithdrawAgent（AI）  — 取款，需要理解自然语言指令
     *   - CreditAgent（AI）    — 存款，需要理解自然语言指令
     *   - ExchangeOperator（非AI）— 汇率换算，纯Java计算，零LLM调用
     *
     * 当用户说"转100欧元"时，监督 Agent 自动判断：
     *   ① 先调 ExchangeOperator（非AI）把 EUR 转 USD
     *   ② 再调 WithdrawAgent（AI）从张三取款
     *   ③ 最后调 CreditAgent（AI）向李四存款
     *
     * 注意观察日志：ExchangeOperator 的输出带 "[非AI·汇率转换]" 标记，
     * 证明它没有调用 LLM。
     */
    @Test
    public void testNonAIAgentWithSupervisor() {

        System.out.println("==============================================================");
        System.out.println("【非 AI Agent 示例】AI + 非AI Agent 混合编排");
        System.out.println("==============================================================");
        System.out.println("初始状态：张三 $1000.00 | 李四 $1000.00");
        System.out.println("--------------------------------------------------------------");
        System.out.println("子 Agent 清单：");
        System.out.println("  WithdrawAgent   — AI Agent  (取款，需LLM理解自然语言)");
        System.out.println("  CreditAgent     — AI Agent  (存款，需LLM理解自然语言)");
        System.out.println("  ExchangeOperator — 非AI Agent (汇率换算，纯Java计算，无LLM)");
        System.out.println("--------------------------------------------------------------\n");

        // 构建取款 Agent（AI）
        WithdrawAgent withdrawAgent = AgenticServices
                .agentBuilder(WithdrawAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        // 构建存款 Agent（AI）
        CreditAgent creditAgent = AgenticServices
                .agentBuilder(CreditAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        // 构建监督 Agent
        // 关键：new ExchangeOperator() 直接作为子 Agent 使用，
        // 无需 AgenticServices 构建器——因为它不是 AI Agent
        SupervisorAgent bankSupervisor = AgenticServices
                .supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(withdrawAgent, creditAgent, new ExchangeOperator())
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

        // 用户只需说目标，监督 Agent 自行决定调用哪些子 Agent
        String result = bankSupervisor.invoke(
                "请帮我把 100 欧元从张三的账户转到李四的账户。");

        System.out.println("\n--------------------------------------------------------------");
        System.out.println("最终账户状态：");
        System.out.printf("  张三：$%.2f%n", bankTool.getBalance("张三"));
        System.out.printf("  李四：$%.2f%n", bankTool.getBalance("李四"));
        System.out.println("==============================================================");
        System.out.println("【监督 Agent 执行摘要】");
        System.out.println(result);
        System.out.println("==============================================================");
    }
}
