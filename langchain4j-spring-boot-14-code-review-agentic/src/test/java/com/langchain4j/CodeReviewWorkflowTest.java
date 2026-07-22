package com.langchain4j;

import com.langchain4j.agent.*;
import com.langchain4j.config.AgenticConfig;
import com.langchain4j.domain.*;
import com.langchain4j.nonai.StaticAnalyzer;
import com.langchain4j.util.CodeLoader;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.Executors;

/**
 * End-to-end integration test for the 6-pattern code review workflow.
 * <p>
 * Orchestrates all workflow patterns:
 * 1. Sequential -- CodeParser -> IssueIdentifier
 * 2. Non-AI Agent -- StaticAnalyzer (rule engine, no LLM)
 * 3. Parallel -- SecurityReviewer + PerformanceReviewer + MaintainabilityReviewer
 * 4. Conditional -- risk-level routing (high risk -> HITL, else auto-approve)
 * 5. Loop -- CodeFixer -> ReReviewer (up to 3 rounds)
 * 6. Final Report -- composite output of all stages
 */
@SpringBootTest
public class CodeReviewWorkflowTest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    // Sequentials agents (from AgenticConfig -- already-built beans)
    @Autowired
    private CodeParser codeParser;
    @Autowired
    private IssueIdentifier issueIdentifier;

    // Parallel agents
    @Autowired
    private SecurityReviewer securityReviewer;
    @Autowired
    private PerformanceReviewer performanceReviewer;
    @Autowired
    private MaintainabilityReviewer maintainabilityReviewer;

    // Loop agents
    @Autowired
    private CodeFixer codeFixer;
    @Autowired
    private ReReviewer reReviewer;

    // Non-AI agent
    @Autowired
    private StaticAnalyzer staticAnalyzer;

    @Test
    public void testFullReviewWorkflow() throws Exception {
        // ====================================================================
        // 0. Configuration: choose a sample file
        // ====================================================================
        String sampleFile = "sample-code/UserService.java";
        String sourceCode = CodeLoader.load(sampleFile);

        System.out.println("=== Code Review Workflow ===");
        System.out.println("File: " + sampleFile);
        System.out.println("Full Review\n");

        // ====================================================================
        // 1. Sequential Workflow: CodeParser -> IssueIdentifier
        //    The two agents run sequentially: parser output feeds into identifier.
        //    Note: the beans from AgenticConfig are already built with outputKey
        //    "parsedCode" (parser) and "codeIssues" (identifier). The identifier
        //    reads "parsedCode" and "sourceCode" from scope.
        // ====================================================================
        System.out.println("--- Step 1: Sequential (Parse -> Identify) ---");

        UntypedAgent parseAndIdentify = AgenticServices
                .sequenceBuilder()
                .subAgents(codeParser, issueIdentifier)
                .outputKey("codeIssues")
                .build();

        Map<String, Object> sequentialInput = Map.of(
                "sourceCode", sourceCode
        );

        String codeIssues = (String) parseAndIdentify.invoke(sequentialInput);
        System.out.println("[Sequential] Code parsing and issue identification complete.\n");

        // ====================================================================
        // 2. Non-AI Agent: StaticAnalyzer (rule engine, deterministic)
        //    Called as a plain Java method -- no LLM involved.
        // ====================================================================
        System.out.println("--- Step 2: Non-AI Agent (Static Analysis) ---");

        StaticAnalysisResult staticResult = staticAnalyzer.analyze(sourceCode);

        System.out.println("[StaticAnalyzer] Cyclomatic complexity: " + staticResult.getCyclomaticComplexity()
                + (Boolean.TRUE.equals(staticResult.getComplexityWarning()) ? " (WARNING)" : " (OK)"));
        System.out.println("[StaticAnalyzer] Longest method: " + staticResult.getMaxMethodLines() + " lines"
                + (Boolean.TRUE.equals(staticResult.getMethodLengthWarning()) ? " (WARNING)" : " (OK)"));
        if (staticResult.getNamingIssues() != null && !staticResult.getNamingIssues().isEmpty()) {
            System.out.println("[StaticAnalyzer] Naming issues: " + staticResult.getNamingIssues().size());
            staticResult.getNamingIssues().forEach(issue -> System.out.println("  - " + issue));
        }
        System.out.println();

        // ====================================================================
        // 3. Parallel Workflow: Security + Performance + Maintainability reviews
        //    Three agents run concurrently in separate threads.
        // ====================================================================
        System.out.println("--- Step 3: Parallel (3-Dimensional Review) ---");

        var executor = Executors.newFixedThreadPool(3);

        UntypedAgent parallelReview = AgenticServices
                .parallelBuilder()
                .subAgents(securityReviewer, performanceReviewer, maintainabilityReviewer)
                .executor(executor)
                .outputKey("combinedReview")
                .output(scope -> {
                    String securityResult = (String) scope.readState("securityReview");
                    String perfResult = (String) scope.readState("perfReview");
                    String maintResult = (String) scope.readState("maintReview");

                    System.out.println("[SecurityReviewer] complete");
                    System.out.println("[PerformanceReviewer] complete");
                    System.out.println("[MaintainabilityReviewer] complete\n");

                    return (securityResult != null ? securityResult : "")
                            + "\n---\n"
                            + (perfResult != null ? perfResult : "")
                            + "\n---\n"
                            + (maintResult != null ? maintResult : "");
                })
                .build();

        Map<String, Object> parallelInput = Map.of(
                "sourceCode", sourceCode,
                "codeIssues", codeIssues != null ? codeIssues : ""
        );

        String combinedReview = (String) parallelReview.invoke(parallelInput);

        // ====================================================================
        // 4. Conditional: Risk-level routing
        //    High-risk findings trigger a Human-in-the-Loop checkpoint.
        //    Otherwise the workflow proceeds with automatic fix.
        // ====================================================================
        System.out.println("--- Step 4: Conditional (Risk Routing) ---");

        String combinedReviewLower = combinedReview != null ? combinedReview.toLowerCase() : "";
        boolean isHighRisk = combinedReviewLower.contains("sql 注入")
                || combinedReviewLower.contains("sql注入")
                || combinedReviewLower.contains("sensitive information")
                || combinedReviewLower.contains("hardcoded key")
                || combinedReviewLower.contains("hardcoded secret")
                || combinedReviewLower.contains("command injection");

        if (isHighRisk) {
            System.out.println("[Conditional] HIGH-RISK issues detected!");
            System.out.println("[Human-in-the-Loop] Escalating for manual approval...");
            System.out.println("[Human-in-the-Loop] <simulated> User approved fixes.\n");
        } else {
            System.out.println("[Conditional] Risk level: MEDIUM/LOW -- proceeding with auto-fix.\n");
        }

        // ====================================================================
        // 5. Loop Workflow: Fix -> Re-review (up to 3 iterations)
        //    The CodeFixer produces fixed code, then ReReviewer scores it.
        //    The loop exits when quality is sufficient or max iterations reached.
        // ====================================================================
        System.out.println("--- Step 5: Loop (Fix -> Re-review) ---");

        UntypedAgent fixLoop = AgenticServices
                .loopBuilder()
                .subAgents(codeFixer, reReviewer)
                .outputKey("finalReview")
                .exitCondition(scope -> {
                    String result = (String) scope.readState("finalReview");
                    if (result != null) {
                        String preview = result.length() > 200 ? result.substring(0, 200) + "..." : result;
                        System.out.println("[Loop] Re-review result: " + preview);
                    }
                    // Let maxIterations control the exit
                    return false;
                })
                .maxIterations(3)
                .build();

        Map<String, Object> loopInput = Map.of(
                "sourceCode", sourceCode,
                "combinedReview", combinedReview != null ? combinedReview : ""
        );

        String finalReview = (String) fixLoop.invoke(loopInput);

        // ====================================================================
        // 6. Generate Final Report
        // ====================================================================
        System.out.println("\n========================================");
        System.out.println("           FINAL REVIEW REPORT");
        System.out.println("========================================");
        System.out.println("File: " + sampleFile);
        System.out.println();
        System.out.println("--- Static Analysis ---");
        System.out.println("  Cyclomatic complexity: " + staticResult.getCyclomaticComplexity()
                + (Boolean.TRUE.equals(staticResult.getComplexityWarning()) ? " [EXCEEDS THRESHOLD]" : " [OK]"));
        System.out.println("  Longest method: " + staticResult.getMaxMethodLines() + " lines"
                + (Boolean.TRUE.equals(staticResult.getMethodLengthWarning()) ? " [EXCEEDS THRESHOLD]" : " [OK]"));
        if (staticResult.getNamingIssues() != null && !staticResult.getNamingIssues().isEmpty()) {
            System.out.println("  Naming issues: " + staticResult.getNamingIssues().size());
        }
        System.out.println();
        System.out.println("--- AI Review ---");
        System.out.println("  Issues identified: "
                + (codeIssues != null ? codeIssues.length() + " chars" : "N/A"));
        System.out.println("  Parallel reviews: Security, Performance, Maintainability");
        System.out.println();
        System.out.println("--- Conditional Routing ---");
        System.out.println("  Risk level: " + (isHighRisk ? "HIGH" : "MEDIUM/LOW"));
        System.out.println("  Human approval: " + (isHighRisk ? "Confirmed (simulated)" : "Not required"));
        System.out.println();
        System.out.println("--- Fix Loop ---");
        System.out.println("  Max iterations: 3");
        System.out.println("  Final review: "
                + (finalReview != null ? finalReview.substring(0, Math.min(100, finalReview.length())) + "..." : "N/A"));
        System.out.println();
        System.out.println("========================================");
        System.out.println("            REVIEW COMPLETE");
        System.out.println("========================================");

        // Clean up
        executor.shutdown();
    }
}
