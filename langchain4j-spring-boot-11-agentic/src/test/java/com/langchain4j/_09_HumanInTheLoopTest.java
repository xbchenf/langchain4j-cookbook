package com.langchain4j;

import com.langchain4j.agentic._09_human_in_the_loop.DecisionsReachedService;
import com.langchain4j.agentic._09_human_in_the_loop.HiringDecisionProposer;
import com.langchain4j.agentic._09_human_in_the_loop.MeetingProposer;
import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * 人工介入工作流测试类 - 测试包含人工验证和交互的工作流
 * 
 * 本示例演示了两种人工介入模式：
 * 1. 简单验证器：AI 提议决策，人类进行最终确认
 * 2. 带记忆的聊天机器人：与人类进行多轮对话，直到达成目标
 */
@SpringBootTest
public class _09_HumanInTheLoopTest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 测试简单的人工验证器
     * AI 代理提议招聘决策，人类进行最终确认
     */
    @Test
    public void testHumanInTheLoopSimpleValidator() throws Exception{
        // 1. 创建涉及的代理
        HiringDecisionProposer decisionProposer = AgenticServices.agentBuilder(HiringDecisionProposer.class)
                .chatModel(openAiChatModel)
                .outputKey("request")  // 修改为 "request"，与人工验证器读取的 key 保持一致
                .build();

        // 2. 定义用于验证的人工介入代理
        HumanInTheLoop humanValidator = AgenticServices.humanInTheLoopBuilder()
                .description("验证模型提议的招聘决策")
                .outputKey("finalDecision") // 由人类检查
                .responseProvider(scope -> {
                    System.out.println("AI 招聘助手建议：" + scope.readState("request"));
                    System.out.println("请确认最终决策。");
                    System.out.println("选项：邀请现场面试 (I)、拒绝 (R)、保留 (H)");
                    System.out.print("> "); // 在实际系统中需要输入验证和错误处理
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                        return reader.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException("读取输入失败", e);
                    }
                })
                .build();

        // 3. 将代理链接到工作流中
        UntypedAgent hiringDecisionWorkflow = AgenticServices.sequenceBuilder()
                .subAgents(decisionProposer, humanValidator)
                .outputKey("finalDecision")
                .build();

        // 4. 准备输入参数
        Map<String, Object> input = Map.of(
                "cvReview", new CvReview(0.85,
                        """
                                技术技能强，除了所需的 React 经验外。
                                似乎是一个快速且独立的学习者。文化契合度好。
                                工作许可存在潜在问题，但似乎可以解决。
                                薪资期望略高于计划预算。
                                决定继续进行现场面试。
                                """)
        );

        // 5. 运行工作流
        String finalDecision = (String) hiringDecisionWorkflow.invoke(input);

        System.out.println("\n=== 人类的最终决策 ===");
        System.out.println("(邀请现场面试 (I)、拒绝 (R)、保留 (H))\n");
        System.out.println(finalDecision);

        // 注意：人工介入和人工验证通常需要用户很长时间才能响应。
        // 在这种情况下，建议使用异步代理，这样它们不会阻塞工作流的其余部分
        // 这些部分可能在用户回答到来之前执行。

    }

    /**
     * 此示例演示了带有人工介入交互的来回循环，
     * 直到达到目标（退出条件），之后工作流的其余部分可以继续。
     * 循环持续进行，直到人类确认可用性，这由 AiService 验证。
     * 如果未找到时间段，循环将在 5 次迭代后结束。
     */
    @Test
    public void testHumanInTheLoopChatbotWithMemory() throws Exception{

        // 1. 定义子代理
        MeetingProposer proposer = AgenticServices
                .agentBuilder(MeetingProposer.class)
                .chatModel(openAiChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(15)) // 让代理记住他已经提议过的内容
                .outputKey("proposal")
                .build();

        // 2. 添加一个 AiService 来判断是否已达成决策（这可以是一个小型本地模型，因为任务非常简单）
        DecisionsReachedService decisionService = AiServices.create(DecisionsReachedService.class, openAiChatModel);

        // 3. 定义人工介入代理
        HumanInTheLoop humanInTheLoop = AgenticServices
                .humanInTheLoopBuilder()
                .description("向用户请求输入的代理")
                .outputKey("candidateAnswer") // 与提议者的一个输入变量名匹配
                .responseProvider(scope -> {
                    System.out.println(scope.readState("proposal")); // 读取 MeetingProposer 的输出
                    System.out.print("> ");
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                        return reader.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException("读取输入失败", e);
                    }
                })
                .async(true) // 等待用户输入时无需阻塞整个程序
                .build();

        // 4. 构建循环
        // 这里我们只希望每个循环检查一次退出条件，而不是在每次代理调用后检查，
        // 所以我们将两个代理捆绑在一个序列中，并作为一个代理提供给循环
        UntypedAgent agentSequence = AgenticServices
                .sequenceBuilder()
                .subAgents(proposer, humanInTheLoop)
                .output(agenticScope -> Map.of(
                        "proposal", agenticScope.readState("proposal"),
                        "candidateAnswer", agenticScope.readState("candidateAnswer")
                ))
                .outputKey("proposalAndAnswer")
                // 此输出包含最后的日期提议 + 候选人的回答，这对于后续代理安排会议（或中止尝试）应该足够了
                .build();

        UntypedAgent schedulingLoop = AgenticServices
                .loopBuilder()
                .subAgents(agentSequence)
                .exitCondition(scope -> {
                    System.out.println("--- 检查退出条件 ---");
                    String response = (String) scope.readState("candidateAnswer");
                    String proposal = (String) scope.readState("proposal");
                    return response != null && decisionService.isDecisionReached(proposal, response);
                })
                .outputKey("proposalAndAnswer")
                .maxIterations(5)
                .build();

        // 5. 运行调度循环
        Map<String, Object> input = Map.of("meetingTopic", "现场访问",
                "candidateAnswer", "hi", // 这个变量需要预先存在于 AgenticScope 中，因为 MeetingProposer 将其作为输入
                "memoryId", "user-1234"); // 如果我们不放置 memoryId，提议者代理将不会记住他已经提议过的内容

        var lastProposalAndAnswer = schedulingLoop.invoke(input);

        System.out.println("=== 结果：最后的提议和回答 ===");
        System.out.println(lastProposalAndAnswer);
    }
    
}
