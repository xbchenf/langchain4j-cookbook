package com.langchain4j;

import com.langchain4j.agentic._03_loop_workflow.CvReviewer;
import com.langchain4j.agentic._03_loop_workflow.ScoredCvTailor;
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

/**
 * 多Agent循环工作流测试类 - 测试将多个Agent组合成循环执行的工作流
 * 本示例演示了如何通过循环评审和定制来持续改进简历，直到达到满意的评分
 */
@SpringBootTest
public class _03_LooplWorkflowTest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 测试无类型代理的循环工作流
     * 使用UntypedAgent创建循环工作流，通过Map传递参数
     */
    @Test
    public void testLoop() throws Exception{

        // 2. 在此包中定义两个子代理：
        //      - CvReviewer.java（简历评审器）
        //      - ScoredCvTailor.java（带评分的简历定制器）

        // 3. 使用 AgenticServices 创建所有代理
        CvReviewer cvReviewer = AgenticServices.agentBuilder(CvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("cvReview") // 这会在每次迭代中更新，为下一次定制提供新的反馈
                .build();
        ScoredCvTailor scoredCvTailor = AgenticServices.agentBuilder(ScoredCvTailor.class)
                .chatModel(openAiChatModel)
                .outputKey("cv") // 这会在每次迭代中更新，持续改进简历
                .build();

        // 4. 构建循环工作流
        UntypedAgent reviewedCvGenerator = AgenticServices // 除非定义结果组合代理，否则使用 UntypedAgent，见 _2_Sequential_Agent_Example
                .loopBuilder().subAgents(cvReviewer, scoredCvTailor) // 可以添加任意数量的子代理，顺序很重要
                .outputKey("cv") // 这是我们想要观察的最终输出（改进后的简历）
                .exitCondition(agenticScope -> {
                    CvReview review = (CvReview) agenticScope.readState("cvReview");
                    System.out.println("检查退出条件，评分=" + review.score); // 我们记录中间评分
                    return review.score > 0.8;
                }) // 基于 CvReviewer 代理给出的评分的退出条件，当 > 0.8 时我们满意
                // 注意：退出条件在每次代理调用后都会检查，而不仅仅是在整个循环结束后
                .maxIterations(3) // 安全措施，避免无限循环，以防退出条件永远无法满足
                .build();

        // 5. 从 resources/documents/ 中的文本文件加载原始参数
        // - master_cv.txt（主简历）
        // - job_description_backend.txt（后端职位描述）
        String masterCv = StringLoader.loadFromResource("/documents/master_cv.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");

        // 5. 因为我们使用无类型代理，所以需要传递一个参数Map
        Map<String, Object> arguments = Map.of(
                "cv", masterCv, // 从主简历开始，它会被持续改进
                "jobDescription", jobDescription
        );

        // 5. 调用组合代理生成定制简历
        String tailoredCv = (String) reviewedCvGenerator.invoke(arguments);

        // 6. 打印生成的简历
        System.out.println("=== 评审后的简历（无类型） ===");
        System.out.println((String) tailoredCv);

        // 这份简历可能在第一次定制+评审回合后就通过了
        // 如果你想看到失败的情况，可以尝试使用长笛老师职位描述
        // 如下示例所示，我们还可以检查简历的中间状态
        // 并获取最终的评审和评分。

    }


    /**
     * 测试类型化代理的循环工作流
     * 使用类型化接口创建循环工作流，可以访问完整的代理作用域
     */
    @Test
    public void testLoop2() throws Exception{
        // 1. 创建所有子代理（与之前相同）
        CvReviewer cvReviewer = AgenticServices.agentBuilder(CvReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("cvReview") // 这会在每次迭代中更新，为下一次定制提供新的反馈
                .build();
        ScoredCvTailor scoredCvTailor = AgenticServices.agentBuilder(ScoredCvTailor.class)
                .chatModel(openAiChatModel)
                .outputKey("cv") // 这会在每次迭代中更新，持续改进简历
                .build();

        // 2. 构建循环工作流并在每次退出条件检查时存储评审结果
        // 了解退出条件是否满足或只是达到最大迭代次数可能很重要
        //（例如，John 可能甚至不想申请这份工作）。
        // 你可以更改输出变量以也包含最后的评分和反馈，并在循环结束后自行检查。
        // 你还可以将中间值存储在可变列表中以供以后检查。
        // 下面的代码同时做了这两件事。
        List<CvReview> reviewHistory = new ArrayList<>();

        UntypedAgent reviewedCvGenerator = AgenticServices // 除非定义结果组合代理，否则使用 UntypedAgent，见下文
                .loopBuilder().subAgents(cvReviewer, scoredCvTailor) // 可以添加任意数量的子代理，顺序很重要
                .outputKey("cvAndReview") // 这是我们想要观察的最终输出
                .output(agenticScope -> {
                    Map<String, Object> cvAndReview = Map.of(
                            "cv", agenticScope.readState("cv"),
                            "finalReview", agenticScope.readState("cvReview")
                    );
                    return cvAndReview;
                })
                .exitCondition(scope -> {
                    CvReview review = (CvReview) scope.readState("cvReview");
                    reviewHistory.add(review); // 在每次代理调用时捕获评分+反馈
                    System.out.println("退出检查，评分=" + review.score);
                    return review.score >= 0.8;
                })
                .maxIterations(3) // 安全措施，避免无限循环，以防退出条件永远无法满足
                .build();

        // 3. 从 resources/documents/ 中的文本文件加载原始参数
        // - master_cv.txt（主简历）
        // - job_description_backend.txt（后端职位描述）
        String masterCv = StringLoader.loadFromResource("/documents/master_cv.txt");
        String fluteJobDescription = "我们正在寻找一位热情的长笛老师加入我们的音乐学院。";

        // 4. 因为我们使用无类型代理，所以需要传递一个参数Map
        Map<String, Object> arguments = Map.of(
                "cv", masterCv, // 从主简历开始，它会被持续改进
                "jobDescription", fluteJobDescription
        );

        // 5. 调用组合代理生成定制简历
        Map<String, Object> cvAndReview = (Map<String, Object>) reviewedCvGenerator.invoke(arguments);

        // 你可以在日志中观察到步骤，例如：
        // 第 1 轮输出："content": "{\n  \"score\": 0.0,\n  \"feedback\": \"这份简历不适合我们音乐学院的长笛老师职位...
        // 第 2 轮输出："content": "{\n  \"score\": 0.3,\n  \"feedback\": \"John 的简历展示了强大的软技能，如沟通、耐心和适应能力，这些在教学角色中很重要。然而，缺乏正式的音乐培训或...
        // 第 3 轮输出："content": "{\n  \"score\": 0.4,\n  \"feedback\": \"John Doe 展示了强大的软技能和指导经验,...

        System.out.println("=== 长笛老师的评审后简历 ===");
        System.out.println(cvAndReview.get("cv")); // 循环结束后的最终简历

        // 现在你在输出Map中获得了 finalReview，所以你可以检查
        // 最终评分和反馈是否满足你的要求
        CvReview review = (CvReview) cvAndReview.get("finalReview");
        System.out.println("=== 长笛老师的最终评审 ===");
        System.out.println("简历" + (review.score >= 0.8 ? "通过" : "未通过") + "，评分=" + review.score);
        System.out.println("最终反馈：" + review.feedback);

        // 在 reviewHistory 中可以找到完整的评审历史
        System.out.println("=== 长笛老师的完整评审历史 ===");
        System.out.println(reviewHistory);
    }
}
