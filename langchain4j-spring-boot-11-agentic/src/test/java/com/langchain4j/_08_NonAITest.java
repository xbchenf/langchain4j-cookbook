package com.langchain4j;

import com.langchain4j.agentic._04_parallel_workflow.HrCvReviewer;
import com.langchain4j.agentic._04_parallel_workflow.ManagerCvReviewer;
import com.langchain4j.agentic._04_parallel_workflow.TeamMemberCvReviewer;
import com.langchain4j.agentic._05_conditional_workflow.EmailAssistant;
import com.langchain4j.agentic._05_conditional_workflow.InterviewOrganizer;
import com.langchain4j.agentic._05_conditional_workflow.OrganizingTools;
import com.langchain4j.agentic._07_supervisor_orchestration.HiringSupervisor;
import com.langchain4j.agentic._08_non_ai_agents.ScoreAggregator;
import com.langchain4j.agentic._08_non_ai_agents.StatusUpdate;
import com.langchain4j.domain.CvReview;
import com.langchain4j.util.StringLoader;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 非 AI 代理测试类 - 测试如何在代理工作流中使用非 AI 代理（普通 Java 方法）
 * 
 * 非 AI 代理只是简单的方法，但可以像其他类型的代理一样使用。
 * 它们非常适合确定性操作，如计算、数据转换和聚合，
 * 在这些场景中你希望避免 LLM 的参与。
 * 
 * 你可以外包给非 AI 代理的步骤越多，你的工作流就会越快、越准确、越便宜。
 * 在工作流中，如果你希望对某些步骤强制执行确定性，非 AI 代理优于工具。
 * 
 * 在这个示例中，我们希望评审者的综合分数由确定性计算得出，而不是由 LLM 计算。
 * 我们还根据综合分数确定性地更新数据库中的申请状态。
 */
@SpringBootTest
public class _08_NonAITest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 测试非 AI 代理工作流
     * 演示如何将非 AI 代理与 AI 代理结合使用
     */
    @Test
    public void testNonAI() throws Exception{

        // 1. 定义本包中的 ScoreAggregator 非 AI 代理

        // 2. 为并行评审步骤构建 AI 子代理
        HrCvReviewer hrReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("hrReview")
                .build();

        ManagerCvReviewer managerReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("managerReview")
                .build();

        TeamMemberCvReviewer teamReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("teamMemberReview")
                .build();

        // 3. 构建组合的并行代理
        var executor = Executors.newFixedThreadPool(3);  // 保持引用以便稍后关闭

        UntypedAgent parallelReviewWorkflow = AgenticServices
                .parallelBuilder()
                .subAgents(hrReviewer, managerReviewer, teamReviewer)
                .executor(executor)
                .build();

        // 4. 构建包含非 AI 代理的完整工作流
        UntypedAgent collectFeedback = AgenticServices
                .sequenceBuilder()
                .subAgents(
                        parallelReviewWorkflow,
                        new ScoreAggregator(), // 非 AI 代理不需要 AgenticServices 构建器。outputKey 'combinedCvReview' 在类中定义
                        new StatusUpdate(), // 接受 'combinedCvReview' 作为输入，不需要输出
                        AgenticServices.agentAction(agenticScope -> { // 另一种添加可以操作 AgenticScope 的非 AI 代理的方式
                            CvReview review = (CvReview) agenticScope.readState("combinedCvReview");
                            agenticScope.writeState("scoreAsPercentage", review.score * 100); // 当来自不同系统的代理通信时，通常需要进行输出转换
                        })
                )
                .outputKey("scoreAsPercentage") // outputKey 在 ScoreAggregator.java 的非 AI 代理注解中定义
                .build();

        // 5. 加载输入数据
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String hrRequirements = StringLoader.loadFromResource("/documents/hr_requirements.txt");
        String phoneInterviewNotes = StringLoader.loadFromResource("/documents/phone_interview_notes.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");

        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "candidateContact", candidateContact,
                "hrRequirements", hrRequirements,
                "phoneInterviewNotes", phoneInterviewNotes,
                "jobDescription", jobDescription
        );

        // 6. 调用工作流
        double scoreAsPercentage = (double) collectFeedback.invoke(arguments);
        executor.shutdown();

        System.out.println("=== 分数百分比 ===");
        System.out.println(scoreAsPercentage);
        // 正如我们在日志中看到的，申请状态也已相应更新

    }


}
