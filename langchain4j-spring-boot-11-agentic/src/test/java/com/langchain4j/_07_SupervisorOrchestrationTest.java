package com.langchain4j;

import com.langchain4j.agentic._04_parallel_workflow.HrCvReviewer;
import com.langchain4j.agentic._04_parallel_workflow.ManagerCvReviewer;
import com.langchain4j.agentic._04_parallel_workflow.TeamMemberCvReviewer;
import com.langchain4j.agentic._05_conditional_workflow.EmailAssistant;
import com.langchain4j.agentic._05_conditional_workflow.InterviewOrganizer;
import com.langchain4j.agentic._05_conditional_workflow.OrganizingTools;
import com.langchain4j.agentic._07_supervisor_orchestration.HiringSupervisor;
import com.langchain4j.util.StringLoader;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 主管编排工作流测试类 - 测试由主管代理动态协调子代理执行的工作流
 * 
 * 纯代理式 AI（Pure Agentic AI）：
 * 到目前为止，我们构建了确定性工作流：顺序、并行、条件、循环以及它们的组合。
 * 然而，有些情况下代理系统需要更灵活和自适应，允许代理根据上下文和之前的结果
 * 自主决定下一步如何执行。这通常被称为“纯代理式 AI”。
 * 
 * 为此，langchain4j-agentic 模块提供了一个开箱即用的监督代理（supervisor agent），
 * 它可以被赋予一组子代理，并能够自主生成一个计划，决定下一个要调用的代理，
 * 或者判断任务是否已完成。
 * 
 * 在这个示例中，主管协调招聘工作流：
 * 他应该运行 HR/经理/团队评审，然后安排面试或发送拒绝邮件。
 * 就像组合工作流示例的第 2 部分一样，但现在是“自组织”的。
 * 
 * 注意：
 * - 主管超级代理可以像其他超级代理类型一样用于组合工作流中
 * - 此示例使用 GPT-4o-mini 运行大约需要 50 秒
 * - 你可以在 PRETTY 日志中持续看到正在发生的事情
 * - 有一些方法可以加快执行速度，请参见文件末尾的注释
 */
@SpringBootTest
public class _07_SupervisorOrchestrationTest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 测试主管编排工作流（基础版）
     * 主管代理动态决定调用哪些子代理以及调用顺序
     */
    @Test
    public void testSupervisorOrchestration() throws Exception{
        // 1. 定义所有子代理
        HrCvReviewer hrReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("hrReview")
                .build();
        // 重要的是，如果我们对多个代理使用相同的方法名
        // （在本例中：所有评审者都使用 'reviewCv'），我们最好给代理命名，如下所示：
        // @Agent(name = "managerReviewer", description = "根据职位描述评审简历，提供反馈和评分")

        ManagerCvReviewer managerReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("managerReview")
                .build();

        TeamMemberCvReviewer teamReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("teamMemberReview")
                .build();

        InterviewOrganizer interviewOrganizer = AgenticServices.agentBuilder(InterviewOrganizer.class)
                .chatModel(openAiChatModel)
                .tools(new OrganizingTools())
                .build();

        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(openAiChatModel)
                .tools(new OrganizingTools())
                .build();

        // 2. 构建主管代理
        SupervisorAgent hiringSupervisor = AgenticServices.supervisorBuilder()
                .chatModel(openAiChatModel)
                .subAgents(hrReviewer, managerReviewer, teamReviewer, interviewOrganizer, emailAssistant)
                .contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY_AND_SUMMARIZATION)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY) // 我们想要一个发生了什么事的摘要，而不是检索响应
                .supervisorContext("始终使用可用的完整评审团。始终用英语回答。调用代理时，使用纯 JSON（无反引号，换行符使用反斜杠+n）。") // 可选的主管行为上下文
                .build();
        // 重要须知：主管将一次调用一个代理，然后审查他的计划以选择下一个要调用的代理
        // 主管无法并行执行代理
        // 如果代理标记为异步，主管将覆盖该设置（不执行异步）并发出警告

        // 3. 加载候选人简历和职位描述
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String hrRequirements = StringLoader.loadFromResource("/documents/hr_requirements.txt");
        String phoneInterviewNotes = StringLoader.loadFromResource("/documents/phone_interview_notes.txt");

        // 启动计时器
        long start = System.nanoTime();
        // 4. 使用自然语言请求调用主管
        String result = (String) hiringSupervisor.invoke(
                "评估以下候选人：\n" +
                        "候选人简历：\n" + candidateCv + "\n\n" +
                        "候选人联系方式：\n" + candidateContact + "\n\n" +
                        "职位描述：\n" + jobDescription + "\n\n" +
                        "HR 要求：\n" + hrRequirements + "\n\n" +
                        "电话面试笔记：\n" + phoneInterviewNotes
        );
        long end = System.nanoTime();
        double elapsedSeconds = (end - start) / 1_000_000_000.0;
        // 在日志中你会注意到最后调用了代理 'done'，这是主管完成调用系列的方式

        System.out.println("=== 主管运行完成，耗时 " + elapsedSeconds + " 秒 ===");
        System.out.println(result);
    }

    // 高级用例：
    // 参见 testAdvanced了解
    // - 类型化主管
    // - 上下文工程
    // - 输出策略
    // - 调用链观察

    // 关于延迟：
    // 整个流程的运行通常需要超过 60 秒。
    // 解决方案是使用快速推理提供商如 CEREBRAS，
    // 它将在 10 秒内运行整个流程，但会犯更多错误。
    // 要尝试使用 CEREBRAS 的示例，获取密钥（点击开始使用免费 API 密钥）
    // https://inference-docs.cerebras.ai/quickstart
    // 并将其保存在环境变量中为 "CEREBRAS_API_KEY"
    // 然后将第 38 行更改为：
    // private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel("CEREBRAS");


    /**
     * 在此示例中，我们构建了一个与 _7a_Supervisor_Orchestration 类似的主管，
     * 但我们探索了主管的一些额外功能：
     * - 类型化主管
     * - 上下文工程
     * - 输出策略
     * - 调用链观察
     * - 上下文演变检查
     */
    @Test
    public void testAdvanced() throws Exception{

        // 1. 定义子代理
        HrCvReviewer hrReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(openAiChatModel)
                .build();
        ManagerCvReviewer managerReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(openAiChatModel)
                .build();
        TeamMemberCvReviewer teamReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(openAiChatModel)
                .build();
        InterviewOrganizer interviewOrganizer = AgenticServices.agentBuilder(InterviewOrganizer.class)
                .chatModel(openAiChatModel)
                .tools(new OrganizingTools())
                .outputKey("response")
                .build();
        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(openAiChatModel)
                .tools(new OrganizingTools())
                .outputKey("response")
                .build();

        // 2. 构建主管

        HiringSupervisor hiringSupervisor = AgenticServices
                .supervisorBuilder(HiringSupervisor.class)
                .chatModel(openAiChatModel)
                .subAgents(hrReviewer, managerReviewer, teamReviewer, interviewOrganizer, emailAssistant)
                .contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY_AND_SUMMARIZATION)
                // 根据你的主管需要了解子代理做了什么，
                // 你可以选择 contextGenerationStrategy：CHAT_MEMORY、SUMMARIZATION 或 CHAT_MEMORY_AND_SUMMARIZATION
                .responseStrategy(SupervisorResponseStrategy.SCORED) // 此策略使用评分模型来决定是 LAST 响应还是 SUMMARY 最能解决用户请求
                // 这里的输出函数将覆盖响应策略
                .supervisorContext("政策：始终先检查 HR，必要时升级，拒绝低匹配度候选人。")
                .build();

        // 3. 加载输入数据
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String hrRequirements = StringLoader.loadFromResource("/documents/hr_requirements.txt");
        String phoneInterviewNotes = StringLoader.loadFromResource("/documents/phone_interview_notes.txt");

        String request = "评估此候选人并安排面试或发送拒绝邮件。\n"
                + "候选人简历：\n" + candidateCv + "\n"
                + "候选人联系方式：\n" + candidateContact + "\n"
                + "职位描述：\n" + jobDescription + "\n"
                + "HR 要求：\n" + hrRequirements + "\n"
                + "电话面试笔记：\n" + phoneInterviewNotes;

        // 4. 调用主管
        long start = System.nanoTime();
        ResultWithAgenticScope<String> decision = hiringSupervisor.invoke(request, "经理技术评审最重要。");
        long end = System.nanoTime();

        System.out.println("=== 招聘主管完成，耗时 " + ((end - start) / 1_000_000_000.0) + " 秒 ===");
        System.out.println(decision.result());

        // 打印收集的上下文
        System.out.println("\n=== 上下文作为对话 ===");
        System.out.println(decision.agenticScope().contextAsConversation()); // 将在下一个版本中工作

    }
    
}
