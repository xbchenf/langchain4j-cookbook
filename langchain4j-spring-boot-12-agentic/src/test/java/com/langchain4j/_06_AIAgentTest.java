package com.langchain4j;

import com.langchain4j.agentic._06_AIAgent.*;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * AI 监督 Agent 测试类（纯代理式 AI）
 *
 * 与前面所有示例的本质区别：
 *
 *   前面示例（确定性工作流）：          本示例（纯代理式 AI）：
 *   ┌──────────────────────┐          ┌──────────────────────┐
 *   │ 开发者预先编排好      │          │ 只给 Agent 目标和     │
 *   │ 每一步的执行顺序      │          │ 一组可用的子 Agent，  │
 *   │（串行/并行/条件/循环）│          │ Agent 自主决定：      │
 *   │                      │          │ - 先调用谁？          │
 *   │ 顺序是写死的          │          │ - 需要调几次？        │
 *   └──────────────────────┘          │ - 什么时候算完成？    │
 *                                      └──────────────────────┘
 *
 * 监督 Agent（SupervisorAgent）被赋予一组子 Agent 后，能够：
 *   1. 分析用户请求，理解任务目标
 *   2. 自主制定执行计划，决定调用哪些子 Agent 以及调用顺序
 *   3. 根据中间结果动态调整后续步骤
 *   4. 判断任务是否已完成，完成后汇总结果
 *
 * 适用场景：
 * - 任务步骤无法提前确定，需要根据上下文动态决策
 * - 用户指令是开放式的（"帮我把事情办了" 而非 "先做A再做B"）
 * - 需要 Agent 自主编排多个子 Agent 协作完成复杂任务
 */
@SpringBootTest
public class _06_AIAgentTest {

    @Autowired
    private OpenAiChatModel chatModel;

    private BankTool bankTool;

    @BeforeEach
    public void setUp() {
        // 初始化银行账户数据：张三和李四各存 1000 美元
        bankTool = new BankTool();
        bankTool.createAccount("张三", 1000.0);
        bankTool.createAccount("李四", 1000.0);
    }

    /**
     * 测试监督 Agent 自主编排跨境转账
     *
     * 用户指令："从张三账户转 100 欧元到李四账户"
     *
     * 监督 Agent 需要自主推理出以下步骤：
     *   ① 调用汇率Agent：100 EUR → ? USD
     *   ② 调用取款Agent：从张三账户取出对应的美元
     *   ③ 调用存款Agent：向李四账户存入对应的美元
     *
     * 关键：开发者没有告诉 Agent 先做什么后做什么——
     * Agent 自己分析任务并决定执行顺序！
     */
    @Test
    public void testSupervisorAgent() {

        System.out.println("======================================================");
        System.out.println("【AI 监督 Agent】自主编排跨境转账任务");
        System.out.println("======================================================");
        System.out.println("初始状态：张三 $1000.00 | 李四 $1000.00");
        System.out.println("用户指令：从张三账户转 100 欧元到李四账户");
        System.out.println("------------------------------------------------------");
        System.out.println("监督 Agent 开始自主分析任务...\n");

        // 构建取款 Agent，挂载银行工具
        WithdrawAgent withdrawAgent = AgenticServices
                .agentBuilder(WithdrawAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        // 构建存款 Agent，挂载银行工具
        CreditAgent creditAgent = AgenticServices
                .agentBuilder(CreditAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        // 构建汇率 Agent，挂载汇率工具
        ExchangeAgent exchangeAgent = AgenticServices
                .agentBuilder(ExchangeAgent.class)
                .chatModel(chatModel)
                .tools(new ExchangeTool())
                .build();

        // 构建监督 Agent（核心）
        // 将三个子 Agent 全部交给监督 Agent，由它自主决定调用策略
        // responseStrategy 设为 SUMMARY 表示监督 Agent 会在最后汇总执行结果
        SupervisorAgent bankSupervisor = AgenticServices
                .supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

        // 用户只需用自然语言描述目标，监督 Agent 自行分解并执行
        String result = bankSupervisor.invoke(
                "请帮我把 100 欧元从张三的账户转到李四的账户。");

        System.out.println("\n------------------------------------------------------");
        System.out.println("最终账户状态：");
        System.out.printf("  张三：$%.2f%n", bankTool.getBalance("张三"));
        System.out.printf("  李四：$%.2f%n", bankTool.getBalance("李四"));
        System.out.println("======================================================");
        System.out.println("【监督 Agent 执行摘要】");
        System.out.println(result);
        System.out.println("======================================================");
    }
}
