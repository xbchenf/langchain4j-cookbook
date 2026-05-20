package com.langchain4j;

import com.langchain4j.agentic._03_loop_workflow.CodeOptimizer;
import com.langchain4j.agentic._03_loop_workflow.CodeReviewer;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * 循环工作流测试类
 *
 * 演示代码审查与优化循环：
 *   审查评分 → (评分 < 0.8 ? 优化改进 → 重新审查 : 通过)
 *
 * 循环工作流适用于需要反复迭代直到满足质量标准的场景：
 * 文档校对→修改→重审、翻译→审校→润色、设计评审→修改→复审等。
 *
 * 注意：退出条件在每次子 Agent 调用后都会检查，而不仅仅在整轮迭代结束后。
 */
@SpringBootTest
public class _03_LoopWorkflowTest {

    @Autowired
    private OpenAiChatModel chatModel;

    /**
     * 测试循环工作流：审查评分 ⇄ 优化改进
     *
     * 模拟代码审查流程：提供一段有改进空间的代码，
     * 审查员评分后若不达标，优化师进行改进，循环往复直到达标或达到最大迭代次数。
     */
    @Test
    public void testLoopWorkflow() {

        // 构建代码审查员 Agent，输出评分到 "score"
        CodeReviewer codeReviewer = AgenticServices
                .agentBuilder(CodeReviewer.class)
                .chatModel(chatModel)
                .outputKey("score")
                .build();

        // 构建代码优化师 Agent，输出优化后的代码到 "code"
        CodeOptimizer codeOptimizer = AgenticServices
                .agentBuilder(CodeOptimizer.class)
                .chatModel(chatModel)
                .outputKey("code")
                .build();

        // 构建审查-优化循环
        // 循环流程：先审查评分，若不达标则优化代码，然后重新审查
        // 退出条件在每次子 Agent 执行后都会检查
        UntypedAgent reviewLoop = AgenticServices
                .loopBuilder()
                .subAgents(codeReviewer, codeOptimizer)
                .outputKey("code")
                .exitCondition(agenticScope -> {
                    double score = agenticScope.readState("score", 0.0);
                    System.out.println("  [退出检查] 当前评分 = " + score);
                    return score >= 0.8;
                })
                .maxIterations(5)
                .build();

        // 提供一段有改进空间的初始代码
        String initialCode = """
                public class FileUtil {
                    public String readFileToString(String path) {
                        String content = "";
                        try {
                            BufferedReader br = new BufferedReader(new FileReader(path));
                            String line;
                            while ((line = br.readLine()) != null) {
                                content += line;
                            }
                            br.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return content;
                    }
                    public void writeStringToFile(String path, String content) {
                        FileWriter fw = new FileWriter(path);
                        fw.write(content);
                        fw.close();
                    }
                }""";

        // 将初始代码和需求说明传入循环
        Map<String, Object> input = Map.of(
                "code", initialCode
        );

        System.out.println("========================================");
        System.out.println("【循环工作流】审查评分 ⇄ 优化改进");
        System.out.println("退出条件: 评分 >= 0.8 或 迭代次数 >= 5");
        System.out.println("========================================");

        // 执行循环工作流
        String improvedCode = (String) reviewLoop.invoke(input);

        System.out.println("\n========================================");
        System.out.println("【最终交付】经过审查优化的高质量代码");
        System.out.println("========================================");
        System.out.println(improvedCode);
    }
}
