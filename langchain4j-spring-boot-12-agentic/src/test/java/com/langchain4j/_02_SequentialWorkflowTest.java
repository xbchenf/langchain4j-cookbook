package com.langchain4j;

import com.langchain4j.agentic._02_sequential_workflow.RequirementAnalyst;
import com.langchain4j.agentic._02_sequential_workflow.SolutionDesigner;
import com.langchain4j.agentic._02_sequential_workflow.QualityReviewer;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * 顺序工作流测试类
 *
 * 演示企业级技术方案设计流水线：
 *   需求分析 → 方案设计 → 质量审核
 *
 * 适用场景：文档起草→审核→发布、代码生成→审查→优化、
 * 合同起草→法务审核→定稿等需要多步骤串行处理的业务。
 */
@SpringBootTest
public class _02_SequentialWorkflowTest {

    @Autowired
    private OpenAiChatModel chatModel;

    /**
     * 测试顺序工作流：需求分析 → 方案设计 → 质量审核
     *
     * 模拟企业实际场景：用户提出开发一个企业知识库管理系统，
     * 三个 Agent 串行协作，逐步生成高质量的技术方案文档。
     */
    @Test
    public void testSequentialWorkflow() {

        // 构建三个 Agent 实例，每个 Agent 通过同一个 outputKey "document" 传递数据
        RequirementAnalyst requirementAnalyst = AgenticServices
                .agentBuilder(RequirementAnalyst.class)
                .chatModel(chatModel)
                .outputKey("document")
                .build();

        SolutionDesigner solutionDesigner = AgenticServices
                .agentBuilder(SolutionDesigner.class)
                .chatModel(chatModel)
                .outputKey("document")
                .build();

        QualityReviewer qualityReviewer = AgenticServices
                .agentBuilder(QualityReviewer.class)
                .chatModel(chatModel)
                .outputKey("document")
                .build();

        // 构建顺序工作流：三个 Agent 按注册顺序串行执行
        UntypedAgent sequentialAgent = AgenticServices
                .sequenceBuilder()
                .subAgents(requirementAnalyst, solutionDesigner, qualityReviewer)
                .outputKey("document")
                .build();

        // 模拟企业项目需求
        Map<String, Object> input = Map.of(
                "requirement", """
                        公司计划开发一套企业知识库管理系统。核心要求：
                        1. 支持多种格式文档上传和在线预览（PDF、Word、Markdown）
                        2. 支持全文检索，检索响应时间不超过 500ms
                        3. 支持文档版本管理和变更历史追溯
                        4. 支持基于角色的权限控制，系统需支持 1000 并发用户
                        """
        );

        System.out.println("========================================");
        System.out.println("【顺序工作流】需求分析 → 方案设计 → 质量审核");
        System.out.println("========================================");

        // 执行流水线：每个 Agent 的输出自动作为下一个 Agent 的输入
        Object finalDocument = sequentialAgent.invoke(input);

        System.out.println("\n========================================");
        System.out.println("【最终交付】审核通过的技术方案文档");
        System.out.println("========================================");
        System.out.println((String) finalDocument);
    }
}
