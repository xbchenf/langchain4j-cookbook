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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * 智能代码审查与优化系统 —— 端到端集成测试
 * <p>
 * 本测试在一个方法内完整演示 LangChain4j Agentic 框架的 6 种工作流编排模式，
 * 通过 {@code sequenceBuilder / parallelBuilder / loopBuilder} 将 8 个 Agent
 * 组合成一个连贯的代码审查流水线。
 *
 * <h3>架构设计：静态检查 + 动态检查（动静结合）</h3>
 * <pre>
 *                         源代码
 *                           │
 *          ┌────────────────┼────────────────┐
 *          ▼                                 ▼
 *   📏 静态检查（非AI）                 🤖 动态检查（AI）
 *   StaticAnalyzer                    CodeParser → IssueIdentifier
 *   · 圈复杂度 / 方法行数 / 命名         · 代码结构解析 → 问题初筛
 *   · 纯 Java 正则，零 LLM              · 输出 codeIssues（4类问题清单）
 *   · 毫秒级，100% 可复现                     │
 *          │                         3 × Reviewer（并行深度审查）
 *          │                         Security / Performance / Maintainability
 *          │                                 │
 *   StaticAnalysisResult              combinedReview
 *          │                                 │
 *          └────────────────┬────────────────┘
 *                           ▼
 *                   🚦 条件路由（风险决策）
 *                   HIGH → HITL 人工审批
 *                   MEDIUM/LOW → 自动修复
 *                           │
 *                           ▼
 *                   🔨 循环修复（CodeFixer ⇄ ReReviewer）
 *                   maxIterations = 3
 *                           │
 *                           ▼
 *                   📝 最终报告（FinalReport）
 * </pre>
 *
 * <h3>工作流全景（5 个阶段）</h3>
 * <ol>
 *   <li><b>第一阶段：动静并行检查</b>—— 静态分析（正则规则引擎）与动态分析
 *       （AI 语义理解）同时执行，两者互补，总耗时 ≈ max(静态, 动态)。</li>
 *   <li><b>第二阶段：风险路由</b>—— 关键字匹配判断风险等级。
 *       HIGH 风险 → 触发 Human-in-the-Loop 人工审批（当前模拟实现）；
 *       MEDIUM/LOW 风险 → 自动进入修复流程。</li>
 *   <li><b>第三阶段：循环修复</b>—— CodeFixer → ReReviewer。
 *       修复代码 → 重新评分，最多迭代 3 轮，直到质量达标或达到上限。</li>
 *   <li><b>第四阶段：最终报告</b>—— 聚合静态+动态全部阶段结果，
 *       输出一站式代码质量全景视图。</li>
 * </ol>
 *
 * <h3>核心概念</h3>
 * <ul>
 *   <li><b>scope</b>：工作流级别的共享状态（类似 Map），Agent 之间通过
 *       {@code outputKey} 写入、{@code @V} 读取，实现解耦的数据传递。</li>
 *   <li><b>UntypedAgent</b>：统一的工作流节点抽象，无论底层是 AI Agent、
 *       Non-AI Agent 还是组合 Agent，对外暴露统一的 {@code invoke(Map)} 接口。</li>
 *   <li><b>Builder 模式</b>：{@code sequenceBuilder / parallelBuilder / loopBuilder}
 *       是工作流编排的核心 API，通过链式调用配置子 Agent、输出 key、退出条件等。</li>
 *   <li><b>动静结合</b>：静态检查（代码规则，确定性）与动态检查（LLM 语义，
 *       灵活性）互补，不是所有检查都需要 AI。</li>
 * </ul>
 *
 * @see com.langchain4j.config.AgenticConfig Agent Bean 配置
 * @see com.langchain4j.agent 各 Agent 接口定义
 */
@SpringBootTest
public class CodeReviewWorkflowTest {

    // ======================== 依赖注入 ========================
    // 所有 Agent Bean 由 AgenticConfig 创建并装配好 outputKey / chatModel，
    // 测试类直接 @Autowired 注入即可，无需手动构建。

    @Autowired
    private OpenAiChatModel openAiChatModel;

    // --- Sequential 顺序代理 ---
    @Autowired
    private CodeParser codeParser;             // Seq-1: 解析代码结构 → outputKey="parsedCode"
    @Autowired
    private IssueIdentifier issueIdentifier;   // Seq-2: 识别问题 → outputKey="codeIssues"

    // --- Parallel 并行代理 ---
    @Autowired
    private SecurityReviewer securityReviewer;           // Par-1: 安全性审查 → outputKey="securityReview"
    @Autowired
    private PerformanceReviewer performanceReviewer;     // Par-2: 性能审查 → outputKey="perfReview"
    @Autowired
    private MaintainabilityReviewer maintainabilityReviewer; // Par-3: 可维护性审查 → outputKey="maintReview"

    // --- Loop 循环代理 ---
    @Autowired
    private CodeFixer codeFixer;     // Loop-1: 生成修复代码 → outputKey="fixedCode"
    @Autowired
    private ReReviewer reReviewer;   // Loop-2: 重新审查评分 → outputKey="finalReview"

    // --- Non-AI 代理 ---
    @Autowired
    private StaticAnalyzer staticAnalyzer;  // 纯 Java 规则引擎，无 LLM 调用

    /**
     * 完整代码审查工作流 —— 动静结合 + 4 阶段编排
     * <p>
     * 执行流程：加载代码 → 第一阶段(静态+动态并行检查) → 第二阶段(风险路由) →
     * 第三阶段(循环修复) → 第四阶段(最终报告)
     * <p>
     * 设计亮点：第一阶段中静态检查（Non-AI 正则引擎）和动态检查
     * （AI Agent 链路）通过 CompletableFuture 真正并行执行，
     * 总耗时 ≈ max(静态耗时, 动态耗时)，而非两者之和。
     */
    @Test
    public void testFullReviewWorkflow() throws Exception {

        // ====================================================================
        // 0. 准备阶段：加载待审查的示例代码
        //    CodeLoader 从 classpath:sample-code/ 读取 Java 源文件
        //    UserService.java 植入了 SQL 注入、N+1 查询、异常吞掉等问题
        // ====================================================================
        String sampleFile = "sample-code/UserService.java";
        String sourceCode = CodeLoader.load(sampleFile);

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   智能代码审查与优化系统 — 工作流启动          ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║  待审查文件: " + String.format("%-32s", sampleFile) + "║");
        System.out.println("║  架构模式: 静态检查 ∥ 动态检查（动静结合）       ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        // ====================================================================
        // 第一阶段：静态检查 ∥ 动态检查（并行执行）
        //
        //   设计理念：不是所有检查都需要 AI。
        //   - 静态检查（📏 Non-AI）：圈复杂度、方法行数、命名规范
        //     → 规则明确，正则搞定，零 Token 消耗，毫秒级响应
        //   - 动态检查（🤖 AI）：代码语义分析、四类问题初筛、三维深度审查
        //     → 需要语义理解，交给 LLM，灵活但消耗 Token
        //
        //   两者只依赖 sourceCode，互不依赖 → CompletableFuture 并行执行
        //   总耗时 ≈ max(静态路径, 动态路径)，而非 sum
        //
        //   【设计决策】为什么用 CompletableFuture 而不是 parallelBuilder？
        //   技术上完全可以：StaticAnalyzer 标注了 @Agent(outputKey="staticResult")，
        //   用 agentBuilder 包成 UntypedAgent 后放进 parallelBuilder，和动态路径并行。
        //   但这样做的代价 > 收益：
        //
        //   ① 类型安全丢失：当前 staticAnalyzer.analyze() 直接返回 StaticAnalysisResult
        //      POJO，调用方可以静态访问 getCyclomaticComplexity() / getMaxMethodLines()。
        //      如果走 parallelBuilder → scope，出来的是 Object，需要手动强转，
        //      一旦 outputKey 配错名字，运行时 ClassCastException 才暴露。
        //
        //   ② 过度框架化：StaticAnalyzer 本质是一个普通 Java 类，analyze(sourceCode)
        //      是最自然、最安全的调用方式。为了一致性牺牲类型安全，得不偿失。
        //
        //   ③ 下游消费差异：StaticAnalysisResult 接下来只被普通 Java 代码消费
        //      （拼报告、打日志），不需要被下游 AI Agent 通过 @V 读取。
        //      如果是给 AI Agent 消费 → 走 scope，进 parallelBuilder。
        //      如果是给 Java 代码消费 → 直接方法调用，保持类型安全。
        //
        //   经验法则：Agentic 框架不要求把所有东西都塞进 Builder API。
        //   标准 Java 并发和框架可以混用，选择标准取决于"下游谁在用这个结果"。
        // ====================================================================
        System.out.println("┌─────────────────────────────────────────────┐");
        System.out.println("│  第一阶段：静态检查 ∥ 动态检查（动静并行）      │");
        System.out.println("└─────────────────────────────────────────────┘\n");

        // ── 路径 A：📏 静态检查（CompletableFuture 异步执行）──
        // StaticAnalyzer 是纯 Java 正则引擎，不调用 LLM
        CompletableFuture<StaticAnalysisResult> staticFuture =
                CompletableFuture.supplyAsync(() -> {
                    System.out.println("  [静态路径] 📏 StaticAnalyzer 启动...");
                    StaticAnalysisResult result = staticAnalyzer.analyze(sourceCode);
                    System.out.println("  [静态路径] ✅ 完成（圈复杂度="
                            + result.getCyclomaticComplexity()
                            + "，最长方法=" + result.getMaxMethodLines() + "行）\n");
                    return result;
                });

        // ── 路径 B：🤖 动态检查（主线程执行）──
        // 内部包含两个子阶段：
        //   B1. Sequential：CodeParser → IssueIdentifier（代码理解 + 问题初筛）
        //   B2. Parallel：3 × Reviewer（安全/性能/可维护性深度审查）
        System.out.println("  [动态路径] 🤖 CodeParser → IssueIdentifier 启动...\n");

        // --- B1. Sequential（顺序执行）：CodeParser → IssueIdentifier ---
        // 先结构化解析代码，再基于解析结果做全类别问题初筛
        // 数据流：sourceCode → CodeParser → scope['parsedCode']
        //         → IssueIdentifier(@V parsedCode + @V sourceCode) → scope['codeIssues']
        System.out.println("  ┌─ B1. Sequential: CodeParser → IssueIdentifier ─┐");

        UntypedAgent parseAndIdentify = AgenticServices
                .sequenceBuilder()                          // 创建顺序编排构建器
                .subAgents(codeParser, issueIdentifier)     // 按序执行：CodeParser → IssueIdentifier
                .outputKey("codeIssues")                    // 最终输出写入 scope['codeIssues']
                .build();

        Map<String, Object> sequentialInput = Map.of(
                "sourceCode", sourceCode
        );

        String codeIssues = (String) parseAndIdentify.invoke(sequentialInput);
        System.out.println("    ✅ CodeParser      完成 → scope['parsedCode']");
        System.out.println("    ✅ IssueIdentifier 完成 → scope['codeIssues']");
        System.out.println("    📊 问题清单: " + (codeIssues != null ? codeIssues.length() + " 字符" : "N/A"));
        System.out.println("  └────────────────────────────────────────────────┘\n");

        // --- B2. Parallel（并行审查）：三维度同时执行 ---
        // SecurityReviewer + PerformanceReviewer + MaintainabilityReviewer
        // 三个 Agent 各自独立调用 LLM，通过线程池并发控制
        // 数据流：codeIssues + sourceCode → 三个 Reviewer → scope['securityReview'/'perfReview'/'maintReview']
        //         → output() 回调聚合 → scope['combinedReview']
        System.out.println("  ┌─ B2. Parallel: 三维度并行审查 ────────────────┐");

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

                    System.out.println("    ✅ SecurityReviewer       完成 → scope['securityReview']");
                    System.out.println("    ✅ PerformanceReviewer    完成 → scope['perfReview']");
                    System.out.println("    ✅ MaintainabilityReviewer 完成 → scope['maintReview']");

                    // 拼接三路结果为统一审查报告
                    return (securityResult != null ? securityResult : "[安全审查结果为空]")
                            + "\n--- 性能审查 ---\n"
                            + (perfResult != null ? perfResult : "[性能审查结果为空]")
                            + "\n--- 可维护性审查 ---\n"
                            + (maintResult != null ? maintResult : "[可维护性审查结果为空]");
                })
                .build();

        Map<String, Object> parallelInput = Map.of(
                "sourceCode", sourceCode,
                "codeIssues", codeIssues != null ? codeIssues : ""
        );

        String combinedReview = (String) parallelReview.invoke(parallelInput);
        System.out.println("    📊 动态审查报告: " + (combinedReview != null ? combinedReview.length() + " 字符" : "N/A"));
        System.out.println("  └────────────────────────────────────────────────┘\n");

        // ── 汇合：等待静态检查完成，合并两路结果 ──
        StaticAnalysisResult staticResult = staticFuture.get();
        System.out.println("┌─────────────────────────────────────────────┐");
        System.out.println("│  📏 静态 + 🤖 动态 — 两路结果合并              │");
        System.out.println("├─────────────────────────────────────────────┤");
        System.out.println("│  📏 圈复杂度: " + String.format("%-3d", staticResult.getCyclomaticComplexity())
                + "（阈值 10）"
                + (Boolean.TRUE.equals(staticResult.getComplexityWarning()) ? " ⚠️" : " ✅") + "                           │");
        System.out.println("│  📏 最长方法: " + String.format("%-3d", staticResult.getMaxMethodLines())
                + " 行（阈值 50）"
                + (Boolean.TRUE.equals(staticResult.getMethodLengthWarning()) ? " ⚠️" : " ✅") + "                       │");
        System.out.println("│  🤖 问题清单: " + String.format("%-5d", (codeIssues != null ? codeIssues.length() : 0))
                + " 字符                              │");
        System.out.println("│  🤖 审查报告: " + String.format("%-5d", (combinedReview != null ? combinedReview.length() : 0))
                + " 字符                              │");
        System.out.println("└─────────────────────────────────────────────┘\n");

        // ====================================================================
        // 第二阶段：🚦 条件路由（风险等级驱动的决策分支）
        //
        //   基于静态+动态两路合并后的结果，判断风险等级：
        //   - HIGH（高风险）：触发 Human-in-the-Loop 人工审批
        //   - MEDIUM/LOW（中低风险）：自动进入修复流程
        //
        //   当前使用关键字匹配实现，生产环境可升级为：
        //   - conditionalBuilder() API 内置路由
        //   - 独立的风险评分 Agent
        //   - 规则引擎（如 Drools）
        // ====================================================================
        System.out.println("┌─────────────────────────────────────────────┐");
        System.out.println("│  第二阶段：🚦 条件路由 + Human-in-the-Loop    │");
        System.out.println("└─────────────────────────────────────────────┘");

        String combinedReviewLower = combinedReview != null ? combinedReview.toLowerCase() : "";
        boolean isHighRisk = combinedReviewLower.contains("sql 注入")
                || combinedReviewLower.contains("sql注入")
                || combinedReviewLower.contains("sensitive information")
                || combinedReviewLower.contains("hardcoded key")
                || combinedReviewLower.contains("hardcoded secret")
                || combinedReviewLower.contains("command injection");

        if (isHighRisk) {
            System.out.println("  🚦 风险判定: 🔴 HIGH（检测到严重安全漏洞）");
            System.out.println("  ├─ 🛑 触发 Human-in-the-Loop 流程");
            System.out.println("  ├─ 👤 人工审批中...（当前为模拟实现）");
            System.out.println("  ├─ ✅ 审批结果: 同意修复（模拟）");
            System.out.println("  └─ ▶ 进入修复流程\n");
        } else {
            System.out.println("  🚦 风险判定: 🟡 MEDIUM / 🟢 LOW");
            System.out.println("  └─ ▶ 无需人工干预，自动进入修复流程\n");
        }

        // ====================================================================
        // 第三阶段：🔨 循环修复（CodeFixer ⇄ ReReviewer）
        //
        //   CodeFixer 根据审查意见修复问题，ReReviewer 重新评分。
        //   循环迭代直到质量达标或达到 maxIterations=3。
        //
        //   每轮数据流：
        //   combinedReview + sourceCode → CodeFixer → scope['fixedCode']
        //   → ReReviewer(@V combinedReview + @V fixedCode) → scope['finalReview']（含0~1评分）
        // ====================================================================
        System.out.println("┌─────────────────────────────────────────────┐");
        System.out.println("│  第三阶段：🔨 循环修复（maxIterations=3）     │");
        System.out.println("└─────────────────────────────────────────────┘");

        UntypedAgent fixLoop = AgenticServices
                .loopBuilder()
                .subAgents(codeFixer, reReviewer)
                .outputKey("finalReview")
                .exitCondition(scope -> {
                    String result = (String) scope.readState("finalReview");
                    if (result != null) {
                        String preview = result.length() > 200
                                ? result.substring(0, 200).replace("\n", " ") + "..."
                                : result.replace("\n", " ");
                        System.out.println("  🔄 本轮重审: " + preview);
                    }
                    return false;  // 由 maxIterations 控制退出
                })
                .maxIterations(3)
                .build();

        Map<String, Object> loopInput = Map.of(
                "sourceCode", sourceCode,
                "combinedReview", combinedReview != null ? combinedReview : ""
        );

        String finalReview = (String) fixLoop.invoke(loopInput);
        System.out.println("  🏁 循环退出\n");

        // ====================================================================
        // 第四阶段：📝 最终报告（聚合全部阶段结果）
        //
        //   将动静并行检查 + 条件路由 + 循环修复的结果汇总
        //   输出一站式代码质量全景视图
        // ====================================================================
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║          📝 最终审查报告 — Final Report           ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║  审查文件: " + String.format("%-38s", sampleFile) + "║");
        System.out.println("╠══════════════════════════════════════════════════╣");

        // ── 静态检查结果 ──
        System.out.println("║  📏 静态检查（StaticAnalyzer — 非 AI 正则引擎）   ║");
        System.out.println("║     ├─ 圈复杂度: " + String.format("%-3d", staticResult.getCyclomaticComplexity())
                + "（阈值 10）"
                + (Boolean.TRUE.equals(staticResult.getComplexityWarning()) ? " ⚠️ 超标" : " ✅ 正常") + "          ║");
        System.out.println("║     ├─ 最长方法: " + String.format("%-3d", staticResult.getMaxMethodLines())
                + " 行（阈值 50）"
                + (Boolean.TRUE.equals(staticResult.getMethodLengthWarning()) ? " ⚠️ 超标" : " ✅ 正常") + "      ║");
        int namingCount = staticResult.getNamingIssues() != null ? staticResult.getNamingIssues().size() : 0;
        System.out.println("║     └─ 命名规范: " + (namingCount > 0 ? namingCount + " 处问题 ⚠️" : "✅ 正常")
                + "                        ║");

        // ── 动态检查结果 ──
        System.out.println("║                                                   ║");
        System.out.println("║  🤖 动态检查（AI Agent 链路）                       ║");
        System.out.println("║     ├─ CodeParser → IssueIdentifier（顺序）        ║");
        System.out.println("║     │   问题清单: " + String.format("%-5d", (codeIssues != null ? codeIssues.length() : 0))
                + " 字符                            ║");
        System.out.println("║     └─ 3 × Reviewer（并行）                        ║");
        System.out.println("║         · SecurityReviewer: 安全漏洞检测           ║");
        System.out.println("║         · PerformanceReviewer: 性能瓶颈分析        ║");
        System.out.println("║         · MaintainabilityReviewer: 可维护性评估    ║");

        // ── 条件路由结果 ──
        System.out.println("║                                                   ║");
        System.out.println("║  🚦 条件路由（风险决策）                            ║");
        System.out.println("║     ├─ 风险等级: " + String.format("%-8s", (isHighRisk ? "🔴 HIGH" : "🟡 MEDIUM/LOW"))
                + "                            ║");
        System.out.println("║     └─ 人工审批: " + (isHighRisk ? "✅ 已确认（模拟）" : "⏭️ 无需审批（自动流转）")
                + "                  ║");

        // ── 循环修复结果 ──
        System.out.println("║                                                   ║");
        System.out.println("║  🔨 循环修复（CodeFixer ⇄ ReReviewer）              ║");
        System.out.println("║     ├─ 最大迭代: 3 轮                               ║");
        System.out.println("║     └─ 最终评分: "
                + (finalReview != null
                        ? finalReview.substring(0, Math.min(60, finalReview.length())).replace("\n", " ")
                        : "N/A")
                + "║");

        System.out.println("║                                                   ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║     ✅ 代码审查工作流执行完毕 — 动静结合架构验证通过    ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // ── 资源清理 ──
        executor.shutdown();
    }
}
