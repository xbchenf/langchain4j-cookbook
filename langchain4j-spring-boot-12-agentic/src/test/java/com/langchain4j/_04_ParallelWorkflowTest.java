package com.langchain4j;

import com.langchain4j.agentic._04_parallel_workflow.EveningPlan;
import com.langchain4j.agentic._04_parallel_workflow.EveningPlannerAgent;
import com.langchain4j.agentic._04_parallel_workflow.FoodExpert;
import com.langchain4j.agentic._04_parallel_workflow.MovieExpert;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 并行工作流测试类
 *
 * 演示美食推荐和电影推荐两个子 Agent 并发执行：
 *   美食专家 ──┐
 *             ├──→ 晚间活动策划师（合并结果）
 *   电影专家 ──┘
 *
 * 并行工作流适用于以下场景：
 * - 多维度信息采集（同时查询多个数据源）
 * - 多专家协同评估（HR + 技术 + 管理层分别评审）
 * - 多渠道内容生成（同时生成文案、图片、视频脚本）
 *
 * 关键优势：总耗时取决于最慢的子任务，而非所有子任务之和。
 */
@SpringBootTest
public class _04_ParallelWorkflowTest {

    @Autowired
    private OpenAiChatModel chatModel;

    /**
     * 测试并行工作流：美食推荐 + 电影推荐同时执行
     *
     * 模拟用户输入心情"浪漫"，两个专家 Agent 并发工作：
     * - 美食专家独立推荐 3 道菜肴
     * - 电影专家独立推荐 3 部电影
     * - 策划师将两组结果组合成完整的晚间活动方案
     */
    @Test
    public void testParallelWorkflow() {

        // 构建美食专家 Agent，将推荐结果以 key "meals" 存入工作流状态
        FoodExpert foodExpert = AgenticServices
                .agentBuilder(FoodExpert.class)
                .chatModel(chatModel)
                .outputKey("meals")
                .build();

        // 构建电影专家 Agent，将推荐结果以 key "movies" 存入工作流状态
        MovieExpert movieExpert = AgenticServices
                .agentBuilder(MovieExpert.class)
                .chatModel(chatModel)
                .outputKey("movies")
                .build();

        // 构建并行工作流编排器
        // 两个子 Agent 并发执行，各自将结果写入状态后，
        // 由 output 函数合并为完整的 EveningPlan 列表
        EveningPlannerAgent eveningPlannerAgent = AgenticServices
                .parallelBuilder(EveningPlannerAgent.class)
                .subAgents(foodExpert, movieExpert)
                .executor(Executors.newFixedThreadPool(2))
                .outputKey("plans")
                .output(agenticScope -> {
                    // 从工作流状态中读取两个子 Agent 的独立输出
                    List<String> movies = agenticScope.readState("movies", List.of());
                    List<String> meals = agenticScope.readState("meals", List.of());

                    // 将电影和菜肴按顺序配对组合
                    List<EveningPlan> moviesAndMeals = new ArrayList<>();
                    for (int i = 0; i < movies.size(); i++) {
                        if (i >= meals.size()) break;
                        moviesAndMeals.add(new EveningPlan(movies.get(i), meals.get(i)));
                    }
                    return moviesAndMeals;
                })
                .build();

        System.out.println("========================================");
        System.out.println("【并行工作流】美食推荐 + 电影推荐 → 晚间活动方案");
        System.out.println("========================================");

        // 执行并行工作流：传入心情"浪漫"
        List<EveningPlan> plans = eveningPlannerAgent.plan("浪漫");

        System.out.println("\n========================================");
        System.out.println("【最终交付】为您定制的晚间活动方案");
        System.out.println("========================================");
        for (int i = 0; i < plans.size(); i++) {
            EveningPlan plan = plans.get(i);
            System.out.printf("方案 %d：看《%s》，品尝 %s%n", i + 1, plan.movie(), plan.meal());
        }
    }
}
