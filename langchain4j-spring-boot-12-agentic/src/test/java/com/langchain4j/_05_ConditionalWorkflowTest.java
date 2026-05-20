package com.langchain4j;

import com.langchain4j.agentic._05_conditional_workflow.*;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 条件工作流测试类
 *
 * 演示企业智能问答路由系统：
 *   用户提问 → 分类路由 → 条件分支 → 专业Agent回答
 *                    ┌── HR → 人事行政专家
 *   分类路由 ──┤── TECHNICAL → 技术支持专家
 *                    ├── FINANCE → 财务报销专家
 *                    └── UNKNOWN → 直接回复（无法处理）
 *
 * 条件工作流适用于以下场景：
 * - 智能客服工单自动分流（售后 / 投诉 / 咨询）
 * - 邮件自动分发给对应部门处理
 * - 用户意图识别后调用不同的业务接口
 * - 审批流中根据金额大小走不同的审批链
 *
 * 关键优势：根据运行时状态动态选择执行路径，避免无效调用。
 */
@SpringBootTest
public class _05_ConditionalWorkflowTest {

    @Autowired
    private OpenAiChatModel chatModel;

    /**
     * 测试条件工作流：分类路由 → 条件分支 → 专家回答
     *
     * 模拟三种不同类型的用户咨询：
     * - 人事类：询问年假政策
     * - 技术类：VPN 连接故障
     * - 财务类：差旅报销流程
     *
     * 验证系统能否正确分类并路由到对应的专业 Agent。
     */
    @Test
    public void testConditionalWorkflow() {

        // 步骤一：构建分类路由 Agent
        // 输出分类结果到 "category" 状态，供后续条件判断使用
        CategoryRouter categoryRouter = AgenticServices
                .agentBuilder(CategoryRouter.class)
                .chatModel(chatModel)
                .outputKey("category")
                .build();

        // 步骤二：构建三个专业领域 Agent
        // 各自输出到 "answer" 状态，只有被条件激活的那个才会执行
        HrExpert hrExpert = AgenticServices
                .agentBuilder(HrExpert.class)
                .chatModel(chatModel)
                .outputKey("answer")
                .build();

        TechnicalExpert technicalExpert = AgenticServices
                .agentBuilder(TechnicalExpert.class)
                .chatModel(chatModel)
                .outputKey("answer")
                .build();

        FinanceExpert financeExpert = AgenticServices
                .agentBuilder(FinanceExpert.class)
                .chatModel(chatModel)
                .outputKey("answer")
                .build();

        // 步骤三：构建条件路由器
        // 根据 "category" 状态值，动态选择执行哪个专业 Agent
        // 只有一个分支会被激活，其他分支被跳过
        UntypedAgent expertsRouter = AgenticServices
                .conditionalBuilder()
                .subAgents(
                        scope -> scope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.HR,
                        hrExpert)
                .subAgents(
                        scope -> scope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL,
                        technicalExpert)
                .subAgents(
                        scope -> scope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.FINANCE,
                        financeExpert)
                .outputKey("answer")
                .build();

        // 步骤四：构建顺序工作流，将「分类」和「条件路由」串联
        // ExpertRouterAgent 先调用 categoryRouter 分类，
        // 然后 expertsRouter 根据分类结果条件性激活对应专家
        ExpertRouterAgent expertRouterAgent = AgenticServices
                .sequenceBuilder(ExpertRouterAgent.class)
                .subAgents(categoryRouter, expertsRouter)
                .outputKey("answer")
                .build();

        // 场景1：人事行政类咨询
        /*System.out.println("==========================================================");
        System.out.println("【场景1】人事行政类 — 年假政策咨询");
        System.out.println("==========================================================");
        String hrAnswer = expertRouterAgent.ask("我想了解一下公司的年假政策，入职满一年可以休几天年假？");
        System.out.println("智能路由回复：\n" + hrAnswer);*/

        // 场景2：技术支持类咨询
        /*System.out.println("\n==========================================================");
        System.out.println("【场景2】技术支持类 — VPN 连接故障");
        System.out.println("==========================================================");
        String techAnswer = expertRouterAgent.ask("公司VPN突然连不上了，客户端提示'无法建立连接'，请问怎么解决？");
        System.out.println("智能路由回复：\n" + techAnswer);*/

        // 场景3：财务报销类咨询
        System.out.println("\n==========================================================");
        System.out.println("【场景3】财务报销类 — 差旅报销流程");
        System.out.println("==========================================================");
        String financeAnswer = expertRouterAgent.ask("出差回来需要报销差旅费，请问需要准备哪些材料？流程是怎样的？");
        System.out.println("智能路由回复：\n" + financeAnswer);
    }
}
