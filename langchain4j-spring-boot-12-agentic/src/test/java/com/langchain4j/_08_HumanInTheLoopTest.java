package com.langchain4j;

import com.langchain4j.agentic._08_HumanInTheLoop.AstrologyAgent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.swing.JOptionPane;
import java.util.Scanner;

/**
 * 人机协作（Human-In-The-Loop）测试类
 *
 * ## 概念说明
 *
 * 在构建 Agent 系统时，某些场景需要人类参与：
 *   - Agent 发现缺少必要信息 → 向用户提问补充
 *   - Agent 准备执行敏感操作（如发送邮件、扣款）→ 请求用户确认
 *   - Agent 遇到不确定的情况 → 请求人工判断
 *
 * HumanInTheLoop 本质上是一个特殊的"非 AI Agent"：
 *   它不是调用 LLM，而是暂停工作流，向人类用户请求输入，
 *   拿到输入后再继续执行后续步骤。
 *
 * ## 工作流示意
 *
 *   用户："我叫张三，运势如何？"
 *         │
 *         ▼
 *   监督 Agent 分析：需要星座信息！
 *         │
 *         ▼
 *   HumanInTheLoop：输出 "请输入您的星座："
 *         │
 *   用户输入："双子座"
 *         │
 *         ▼
 *   AstrologyAgent：生成 "张三 · 双子座" 的运势
 *         │
 *         ▼
 *   返回完整运势给用户
 *
 * ## 适用场景
 *
 *   - 客服系统：需要用户补充订单号、手机号等信息
 *   - 审批流：AI 起草合同 → 人类审批确认 → 发送
 *   - 数据校验：AI 提取信息 → 不确定时请人类确认
 *   - 交互式助手：多轮对话中逐步收集用户需求
 */
@SpringBootTest
public class _08_HumanInTheLoopTest {

    @Autowired
    private OpenAiChatModel chatModel;

    /**
     * 测试人机协作工作流
     *
     * 场景：用户只提供了姓名，没有说星座。
     * 监督 Agent 发现星座信息缺失，通过 HumanInTheLoop 向用户追问，
     * 用户补充星座后，AstrologyAgent 生成完整的运势分析。
     *
     * 注意：本测试需要用户在控制台输入星座。
     * 在 IDE 中运行时，请关注控制台输出并在提示时输入。
     * 在 CI/自动化环境中，由于无人工输入，此测试不适合自动运行。
     */
    @Test
    public void testHumanInTheLoop() {

        // 构建星座运势生成 Agent（AI Agent）
        AstrologyAgent astrologyAgent = AgenticServices
                .agentBuilder(AstrologyAgent.class)
                .chatModel(chatModel)
                .build();

        // 构建人机交互 Agent（特殊的非 AI Agent）
        // responseProvider 接收当前工作流状态，向用户展示上下文并收集输入
        // 用户的返回值会写入 outputKey("sign") 指定的状态键中
        HumanInTheLoop humanInTheLoop = AgenticServices
                .humanInTheLoopBuilder()
                .description("向用户询问星座信息（白羊座/金牛座/双子座/巨蟹座/狮子座/处女座/天秤座/天蝎座/射手座/摩羯座/水瓶座/双鱼座）")
                .outputKey("sign")
                .responseProvider(scope -> {
                    System.out.println("\n========================================");
                    System.out.println("[系统询问] 请告诉我您的星座是什么？");
                    System.out.println("可选：白羊座/金牛座/双子座/巨蟹座/狮子座/处女座");
                    System.out.println("      天秤座/天蝎座/射手座/摩羯座/水瓶座/双鱼座");
                    System.out.print("[请输入您的星座] > ");
                    // 使用 Scanner 读取控制台输入，兼容 IDEA 等 IDE 环境
                    // 提示：运行时请点击控制台面板，输入星座后按回车
                    //Scanner scanner = new Scanner(System.in);
                    String input = "双子座";//scanner.nextLine();
                    System.out.println("[已收到] 您的星座是：" + input);
                    return input;
                })
                .build();

        // 构建监督 Agent，组合 AI Agent + HumanInTheLoop
        SupervisorAgent horoscopeSupervisor = AgenticServices
                .supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(astrologyAgent, humanInTheLoop)
                .build();

        System.out.println("================================================");
        System.out.println("【人机协作示例】星座运势查询（带人工输入）");
        System.out.println("================================================");

        // 用户只提供了姓名，监督 Agent 会发现缺少星座信息，
        // 自动触发 HumanInTheLoop 向用户追问
        String result = horoscopeSupervisor.invoke("我叫张三，请帮我看看今天的运势如何？");

        System.out.println("\n================================================");
        System.out.println("【最终结果】您的今日运势");
        System.out.println("================================================");
        System.out.println(result);
        System.out.println("================================================");
    }
}
