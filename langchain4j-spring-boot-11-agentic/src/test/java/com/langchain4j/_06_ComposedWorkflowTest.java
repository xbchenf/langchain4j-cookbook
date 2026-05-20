package com.langchain4j;

import com.langchain4j.agentic._01_basic.CvGenerator;
import com.langchain4j.agentic._03_loop_workflow.CvReviewer;
import com.langchain4j.agentic._03_loop_workflow.ScoredCvTailor;
import com.langchain4j.agentic._04_parallel_workflow.HrCvReviewer;
import com.langchain4j.agentic._04_parallel_workflow.ManagerCvReviewer;
import com.langchain4j.agentic._04_parallel_workflow.TeamMemberCvReviewer;
import com.langchain4j.agentic._05_conditional_workflow.*;
import com.langchain4j.agentic._06_composed_workflow.CandidateWorkflow;
import com.langchain4j.agentic._06_composed_workflow.HiringTeamWorkflow;
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
 * 多Agent组合工作流测试类 - 测试将多个工作流组合成更复杂的工作流
 * 本示例演示了如何将候选人工作流和招聘团队工作流组合成一个完整的招聘系统
 */
@SpringBootTest
public class _06_ComposedWorkflowTest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 测试组合工作流
     * 包含两个主要部分：候选人工作流（生成和优化简历）和招聘团队工作流（评审和决策）
     */
    @Test
    public void testComposedWorkflow() throws Exception{
        ////////////////// 候选人组合工作流 //////////////////////
        // 我们将从生活故事 > 简历 > 评审 > 评审循环直到通过
        // 然后将简历通过邮件发送给公司

        // 1. 为候选人工作流创建所有必要的代理
        CvGenerator cvGenerator = AgenticServices
                .agentBuilder(CvGenerator.class)
                .chatModel(openAiChatModel)
                .outputKey("cv")
                .build();

        ScoredCvTailor scoredCvTailor = AgenticServices
                .agentBuilder(ScoredCvTailor.class)
                .chatModel(openAiChatModel)
                .outputKey("cv")
                .build();

        CvReviewer cvReviewer = AgenticServices
                .agentBuilder(CvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("cvReview")
                .build();

        // 2. 创建简历改进的循环工作流
        UntypedAgent cvImprovementLoop = AgenticServices
                .loopBuilder()
                .subAgents(scoredCvTailor, cvReviewer)
                .outputKey("cv")
                .exitCondition(agenticScope -> {
                    CvReview review = (CvReview) agenticScope.readState("cvReview");
                    System.out.println("简历评审评分：" + review.score);
                    if (review.score >= 0.8)
                        System.out.println("简历足够好，退出循环。\n");
                    return review.score >= 0.8;
                })
                .maxIterations(3)
                .build();

        // 3. 创建完整的候选人工作流：生成 > 评审 > 改进循环
        CandidateWorkflow candidateWorkflow = AgenticServices
                .sequenceBuilder(CandidateWorkflow.class)
                .subAgents(cvGenerator, cvReviewer, cvImprovementLoop)
                // 这里我们在 sequenceBuilder 中使用组合代理 cvImprovementLoop
                // 我们还需要 cvReviewer 以便在进入循环之前生成第一次评审
                .outputKey("cv")
                .build();

        // 4. 加载输入数据
        String lifeStory = StringLoader.loadFromResource("/documents/user_life_story.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");

        // 5. 执行候选人工作流
        String candidateResult = candidateWorkflow.processCandidate(lifeStory, jobDescription);
        // 注意：输入参数和中间参数都存储在一个 AgenticScope 中
        // 系统中的所有代理都可以访问，无论我们有多少层组合

        System.out.println("=== 候选人工作流完成 ===");
        System.out.println("最终简历：" + candidateResult);

        System.out.println("\n\n\n\n");

        ////////////////// 招聘团队组合工作流 //////////////////////
        // 我们收到一封包含候选人简历和联系方式的邮件。我们已经进行了 HR 电话面试。
        // 现在我们进行 3 个并行评审，然后将结果送入条件流程以邀请或拒绝。

        // 1. 为招聘团队工作流创建所有必要的代理
        HrCvReviewer hrCvReviewer = AgenticServices
                .agentBuilder(HrCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("hrReview")
                .build();

        ManagerCvReviewer managerCvReviewer = AgenticServices
                .agentBuilder(ManagerCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("managerReview")
                .build();

        TeamMemberCvReviewer teamMemberCvReviewer = AgenticServices
                .agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("teamMemberReview")
                .build();

        EmailAssistant emailAssistant = AgenticServices
                .agentBuilder(EmailAssistant.class)
                .chatModel(openAiChatModel)
                .tools(new OrganizingTools())
                .build();

        InterviewOrganizer interviewOrganizer = AgenticServices
                .agentBuilder(InterviewOrganizer.class)
                .chatModel(openAiChatModel)
                .tools(new OrganizingTools())
                .build();

        // 2. 创建并行评审工作流
        UntypedAgent parallelReviewWorkflow = AgenticServices
                .parallelBuilder()
                .subAgents(hrCvReviewer, managerCvReviewer, teamMemberCvReviewer)
                .executor(Executors.newFixedThreadPool(3))
                .outputKey("combinedCvReview")
                .output(agenticScope -> {
                    CvReview hrReview = (CvReview) agenticScope.readState("hrReview");
                    CvReview managerReview = (CvReview) agenticScope.readState("managerReview");
                    CvReview teamMemberReview = (CvReview) agenticScope.readState("teamMemberReview");
                    String feedback = String.join("\n",
                            "HR 评审：" + hrReview.feedback,
                            "经理评审：" + managerReview.feedback,
                            "团队成员评审：" + teamMemberReview.feedback
                    );
                    double avgScore = (hrReview.score + managerReview.score + teamMemberReview.score) / 3.0;
                    System.out.println("最终平均简历评审评分：" + avgScore + "\n");
                    return new CvReview(avgScore, feedback);
                })
                .build();

        // 3. 创建最终决策的条件工作流
        UntypedAgent decisionWorkflow = AgenticServices
                .conditionalBuilder()
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("combinedCvReview")).score >= 0.8, interviewOrganizer)
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("combinedCvReview")).score < 0.8, emailAssistant)
                .build();

        // 4. 创建完整的招聘团队工作流：并行评审 → 决策
        HiringTeamWorkflow hiringTeamWorkflow = AgenticServices
                .sequenceBuilder(HiringTeamWorkflow.class)
                .subAgents(parallelReviewWorkflow, decisionWorkflow)
                .build();

        // 5. 加载输入数据
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String hrRequirements = StringLoader.loadFromResource("/documents/hr_requirements.txt");
        String phoneInterviewNotes = StringLoader.loadFromResource("/documents/phone_interview_notes.txt");

        // 将所有数据放入 Map 以便于访问
        Map<String, Object> inputData = Map.of(
                "candidateCv", candidateCv,
                "candidateContact", candidateContact,
                "hrRequirements", hrRequirements,
                "phoneInterviewNotes", phoneInterviewNotes,
                "jobDescription", jobDescription
        );

        // 6. 执行招聘团队工作流
        hiringTeamWorkflow.processApplication(candidateCv, jobDescription, hrRequirements, phoneInterviewNotes, candidateContact);

        System.out.println("=== 招聘团队工作流完成 ===");
        System.out.println("并行评审完成并做出决策");

        // 注意：随着工作流变得越来越复杂，请确保输入、中间和输出参数的名称
        // 是唯一的，以避免在共享的 AgenticScope 中意外覆盖数据
    }
}
