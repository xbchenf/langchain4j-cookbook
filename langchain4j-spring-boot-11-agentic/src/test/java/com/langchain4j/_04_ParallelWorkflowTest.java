package com.langchain4j;

import com.langchain4j.agentic._03_loop_workflow.CvReviewer;
import com.langchain4j.agentic._03_loop_workflow.ScoredCvTailor;
import com.langchain4j.agentic._04_parallel_workflow.HrCvReviewer;
import com.langchain4j.agentic._04_parallel_workflow.ManagerCvReviewer;
import com.langchain4j.agentic._04_parallel_workflow.TeamMemberCvReviewer;
import com.langchain4j.domain.CvReview;
import com.langchain4j.util.StringLoader;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 多Agent并行工作流测试类 - 测试将多个Agent组合成并行执行的工作流
 * 本示例演示了如何同时从 HR、经理和团队成员三个角度并行评审简历，然后汇总结果
 */
@SpringBootTest
public class _04_ParallelWorkflowTest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 测试并行工作流
     * 使用并行构建器创建并行工作流，同时执行多个评审代理
     */
    @Test
    public void testParallelWorkflow() throws Exception{
        // 2. 在此包中定义三个子代理：
        //      - HrCvReviewer.java（HR 简历评审器）
        //      - ManagerCvReviewer.java（经理简历评审器）
        //      - TeamMemberCvReviewer.java（团队成员简历评审器）

        // 3. 使用 AgenticServices 创建所有代理
        HrCvReviewer hrCvReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("hrReview") // 这会在每次迭代中被覆盖，也将用作我们想要观察的最终输出
                .build();

        ManagerCvReviewer managerCvReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("managerReview") // 这会覆盖原始输入指令，并在每次迭代中被覆盖，用作 CvTailor 的新指令
                .build();

        TeamMemberCvReviewer teamMemberCvReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("teamMemberReview") // 这会覆盖原始输入指令，并在每次迭代中被覆盖，用作 CvTailor 的新指令
                .build();

        // 4. 构建并行工作流
        var executor = Executors.newFixedThreadPool(3);  // 保持引用以便稍后关闭

        UntypedAgent cvReviewGenerator = AgenticServices // 除非定义结果组合代理，否则使用 UntypedAgent，见 _2_Sequential_Agent_Example
                .parallelBuilder()
                .subAgents(hrCvReviewer, managerCvReviewer, teamMemberCvReviewer) // 可以添加任意数量的子代理
                .executor(executor) // 可选，默认使用内部缓存线程池，执行完成后会自动关闭
                .outputKey("fullCvReview") // 这是我们想要观察的最终输出
                .output(agenticScope -> {
                    // 从代理作用域中读取每个评审器的输出
                    CvReview hrReview = (CvReview) agenticScope.readState("hrReview");
                    CvReview managerReview = (CvReview) agenticScope.readState("managerReview");
                    CvReview teamMemberReview = (CvReview) agenticScope.readState("teamMemberReview");
                    // 返回捆绑的评审结果，包含平均评分（或你想要的任何其他聚合方式）
                    String feedback = String.join("\n",
                            "HR 评审：" + hrReview.feedback,
                            "经理评审：" + managerReview.feedback,
                            "团队成员评审：" + teamMemberReview.feedback
                    );
                    double avgScore = (hrReview.score + managerReview.score + teamMemberReview.score) / 3.0;

                    return new CvReview(avgScore, feedback);
                })
                .build();

        // 5. 从 resources/documents/ 中的文本文件加载原始参数
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        String hrRequirements = StringLoader.loadFromResource("/documents/hr_requirements.txt");
        String phoneInterviewNotes = StringLoader.loadFromResource("/documents/phone_interview_notes.txt");

        // 6. 因为我们使用无类型代理，所以需要传递一个参数Map
        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "jobDescription", jobDescription
                ,"hrRequirements", hrRequirements
                ,"phoneInterviewNotes", phoneInterviewNotes
        );

        // 7. 调用组合代理生成定制简历
        var review = cvReviewGenerator.invoke(arguments);

        // 8. 打印生成的评审结果
        System.out.println("=== 评审结果 ===");
        System.out.println(review);

        // 9. 关闭执行器
        executor.shutdown();

    }

}
