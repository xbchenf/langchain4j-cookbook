package com.langchain4j;

import com.langchain4j.agentic._04_parallel_workflow.HrCvReviewer;
import com.langchain4j.agentic._04_parallel_workflow.ManagerCvReviewer;
import com.langchain4j.agentic._04_parallel_workflow.TeamMemberCvReviewer;
import com.langchain4j.agentic._05_conditional_workflow.*;
import com.langchain4j.domain.CvReview;
import com.langchain4j.util.StringLoader;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 多Agent条件工作流测试类 - 测试根据条件路由到不同Agent的工作流
 * 本示例演示了如何根据简历评审评分决定是安排面试还是发送拒绝邮件
 */
@SpringBootTest
public class _05_ConditionalWorkflowTest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 测试条件工作流（同步）
     * 根据简历评审分数决定执行哪个分支：高分则安排面试，低分则发送拒绝邮件
     */
    @Test
    public void testConditionalWorkflow() throws Exception{
        // 2. 在此包中定义两个子代理：
        //      - EmailAssistant.java（邮件助手 - 发送拒绝邮件）
        //      - InterviewOrganizer.java（面试组织者 - 安排现场面试）

        // 3. 使用 AgenticServices 创建所有代理
        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(openAiChatModel)
                .tools(new OrganizingTools()) // 代理可以使用那里定义的所有工具
                .build();
        InterviewOrganizer interviewOrganizer = AgenticServices.agentBuilder(InterviewOrganizer.class)
                .chatModel(openAiChatModel)
                .tools(new OrganizingTools())
                .contentRetriever(RagProvider.loadHouseRulesRetriever()) // 这是我们为代理添加 RAG 的方式
                .build();

        // 4. 构建条件工作流
        UntypedAgent candidateResponder = AgenticServices // 除非定义结果组合代理，否则使用 UntypedAgent，见 _2_Sequential_Agent_Example
                .conditionalBuilder()
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("cvReview")).score >= 0.8, interviewOrganizer)
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("cvReview")).score < 0.8, emailAssistant)
                .build();
        // 须知：当定义了多个条件时，它们会按顺序依次执行。
        // 如果你想在这里并行执行，请使用异步代理，如 testParallelWorkflow2 中所示

        // 5. 从 resources/documents/ 中的文本文件加载参数
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        CvReview cvReviewFail = new CvReview(0.6, "简历不错，但缺乏后端职位相关的一些技术细节。");
        CvReview cvReviewPass = new CvReview(0.9, "简历优秀，符合后端职位的所有要求。");

        // 5. 因为我们使用无类型代理，所以需要传递一个包含所有输入参数的Map
        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "candidateContact", candidateContact,
                "jobDescription", jobDescription,
                "cvReview", cvReviewPass // 改为 cvReviewFail 以查看另一个分支
        );

        // 5. 调用条件代理以根据评审结果回复候选人
        candidateResponder.invoke(arguments);
        // 在本例中，我们没有对 AgenticScope 进行有意义的更改
        // 也没有有意义的输出要打印，因为工具执行了最终操作。
        // 我们打印到控制台，显示工具采取了哪些操作（发送邮件、更新申请状态）

        // 当你在调试模式下观察日志时，工具调用结果“success”仍然发送给模型
        // 模型仍然会回答类似“邮件已发送给 John Doe，通知他...”的内容

        // 提示：如果工具是你的最后操作，并且你不想之后再次调用模型，
        // 你通常会添加 @Tool(returnBehavior = ReturnBehavior.IMMEDIATE)
        // https://docs.langchain4j.dev/tutorials/tools#returning-immediately-the-result-of-a-tool-execution-request
        // !!! 但在代理工作流中，不推荐对工具使用 IMMEDIATE RETURN BEHAVIOR，
        // 因为立即返回行为会将工具结果存储在 AgenticScope 中，可能会出现问题

        // 提示：这是一个基于代码检查条件的路由行为示例。
        // 路由行为也可以通过让 LLM 确定最佳工具/代理来继续，可以使用
        // - Supervisor 代理：将在代理上运行，见 _7_supervisor_orchestration
        // - AiServices 作为工具，像这样
        // RouterService routerService = AiServices.builder(RouterAgent.class)
        //        .chatModel(model)
        //        .tools(medicalExpert, legalExpert, technicalExpert)
        //        .build();
        //
        // 最佳选项取决于你的用例：
        //
        // - 使用条件代理时，你硬编码调用条件
        // - 而使用 AiServices 或 Supervisor 时，LLM 决定调用哪个专家
        //
        // - 使用代理解决方案（条件、supervisor）时，所有中间状态和调用链都存储在 AgenticScope 中
        // - 而使用 AiServices 时，跟踪调用链或中间状态要困难得多
    }
    
    /**
     * 测试条件工作流（异步）
     * 使用异步代理实现并行条件执行
     */
    @Test
    public void testConditionalWorkflow2() throws Exception{
        // 1. 创建所有异步代理
        ManagerCvReviewer managerCvReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(openAiChatModel)
                .async(true) // 异步代理
                .outputKey("managerReview")
                .build();
        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(openAiChatModel)
                .async(true)
                .tools(new OrganizingTools())
                .outputKey("sentEmailId")
                .build();
        InfoRequester infoRequester = AgenticServices.agentBuilder(InfoRequester.class)
                .chatModel(openAiChatModel)
                .async(true)
                .tools(new OrganizingTools())
                .outputKey("sentEmailId")
                .build();

        // 2. 构建异步条件工作流
        UntypedAgent candidateResponder = AgenticServices
                .conditionalBuilder()
                .subAgents(scope -> {
                    CvReview hrReview = (CvReview) scope.readState("cvReview");
                    return hrReview.score >= 0.8; // 如果 HR 通过，发送给经理评审
                }, managerCvReviewer)
                .subAgents(scope -> {
                    CvReview hrReview = (CvReview) scope.readState("cvReview");
                    return hrReview.score < 0.8; // 如果 HR 未通过，发送拒绝邮件
                }, emailAssistant)
                .subAgents(scope -> {
                    CvReview hrReview = (CvReview) scope.readState("cvReview");
                    return hrReview.feedback.toLowerCase().contains("缺失信息：");
                }, infoRequester) // 如果需要，向候选人请求更多信息
                .output(agenticScope ->
                        (agenticScope.readState("managerReview", new CvReview(0, "无需经理评审"))).toString() +
                                "\n" + agenticScope.readState("sentEmailId", 0)
                ) // 最终输出是经理评审（如果有）
                .build();

        // 3. 输入参数
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        CvReview hrReview = new CvReview(
                0.85,
                """
                        候选人扎实，薪资期望在范围内，能够在期望的时间框架内开始工作。
                        缺失信息：比利时的工授权状态详情。
                        """
        );

        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "candidateContact", candidateContact,
                "jobDescription", jobDescription,
                "cvReview", hrReview
        );


        // 4. 运行异步条件工作流
        candidateResponder.invoke(arguments);

        System.out.println("=== 异步条件工作流执行完成 ===");
    }
}
