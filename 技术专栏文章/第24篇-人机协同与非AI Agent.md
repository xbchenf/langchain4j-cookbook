# LangChain4j Java AI 应用开发实战（二十四）：人机协同与非 AI Agent —— 混合执行系统

> **摘要**：本文深入讲解 LangChain4j Agentic 框架中的两种特殊 Agent 类型——NonAI Agent（纯代码逻辑代理）和 HumanInTheLoop（人工介入代理）。通过完整招聘系统的实战案例，你将掌握如何将确定性 Java 方法与 AI Agent 无缝混排、如何在关键时刻暂停工作流等待人工审批、以及如何设计 AI + 代码 + 人类三者协同的混合执行架构。学完本文，你将能构建更高效、更可靠、更安全的企业级 Agent 系统。

---

## 前言

在前面的六篇文章中，我们已经掌握了 Agentic 工作流编排的全部基础模式：

- ✅ 第18篇：**Agentic AI 入门** — 单步 Agent 与 ReAct 模式
- ✅ 第19篇：**顺序工作流** — 多阶段流水线串联
- ✅ 第20篇：**循环工作流** — 迭代优化直到达标
- ✅ 第21篇：**并行工作流** — 多 Agent 并发协同
- ✅ 第22篇：**条件分支** — 基于规则的路由分发
- ✅ 第23篇：**主管编排与组合工作流** — Supervisor 自主调度与模式嵌套

但截至目前，我们工作流中的每一个步骤**都是 AI Agent**——它们全部调用 LLM 来完成推理。如果你仔细思考，会发现这并不合理：

**场景 1：没必要用 LLM 的确定性计算**

```
HR评审分数：0.85
经理评审分数：0.72
团队评审分数：0.80

需求：计算三者的平均分

用 LLM 算：(0.85 + 0.72 + 0.80) / 3 = ?
  → Token 消耗：~200 tokens
  → 延迟：~1.5 秒
  → 准确率：99.7%（偶有计算错误）

用纯 Java 代码算：
  → Token 消耗：0
  → 延迟：~0.00001 秒
  → 准确率：100%
```

**场景 2：不能交给 AI 的敏感决策**

```
AI 提议："建议录用该候选人，薪资 35K"

问题：谁来做最终决策？
  → 必须是人类 HR ❗
  → 涉及到成本、团队平衡、公司政策
  → 出了问题 AI 无法负责
```

**这就是混合执行系统的价值所在！**

LangChain4j Agentic 框架将 **NonAI Agent**（纯代码逻辑）和 **HumanInTheLoop**（人工介入）都设计为工作流的一等公民，与 AI Agent 无缝混排。本文将带你从原理到实战，完整掌握混合执行架构的设计与实现。

---

## 一、核心概念：混合执行系统的三种 Agent

### 1.1 为什么 AI Agent 不能包办一切？

AI Agent 虽然强大，但有明显的边界：

| 维度 | AI Agent（LLM） | 纯代码 | 人类 |
|------|----------------|--------|------|
| **确定性计算** | ❌ 可能出错 | ✅ 100% 准确 | ✅ |
| **语义理解** | ✅ 核心能力 | ❌ 做不到 | ✅ |
| **执行速度** | 秒级 | 微秒级 | 分钟/小时级 |
| **Token 成本** | 有（每次调用） | 无 | 无 |
| **合规责任** | ❌ 无法担责 | 取决于逻辑 | ✅ 可担责 |
| **复杂判断** | ⚠️ 有限 | ❌ 做不到 | ✅ 核心能力 |

**结论**：一个生产级 Agent 系统应该让**三者各司其职**：

```
┌──────────────────────────────────────────────────┐
│              混合执行工作流                        │
│                                                  │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐     │
│  │ AI Agent │ → │ NonAI    │ → │ Human    │ → … │
│  │ (GPT-4)  │   │ Agent    │   │ Agent    │     │
│  └──────────┘   └──────────┘   └──────────┘     │
│   语义理解       确定性计算       人工决策          │
│   文本生成       数据转换         最终审批          │
│   推理判断       业务规则        敏感操作确认       │
└──────────────────────────────────────────────────┘
```

### 1.2 三种 Agent 类型对比

| 特性 | AI Agent | NonAI Agent | HumanInTheLoop |
|------|---------|-------------|----------------|
| **本质** | `@Agent` 接口 + LLM 推理 | `@Agent` 注解的 Java 方法 | `humanInTheLoopBuilder()` 构建 |
| **执行方式** | 调用 ChatModel | 直接执行 Java 代码 | 暂停等待人工输入 |
| **输入参数** | `@V` 从 AgenticScope 绑定 | `@V` 从 AgenticScope 绑定 | `responseProvider` 回调 |
| **输出** | LLM 生成 | Java 方法返回值 | 用户输入字符串 |
| **适用场景** | 语义理解、文本生成、推理 | 计算、转换、验证、规则判断 | 审批、确认、信息补充 |
| **速度** | 1-3 秒 | < 1 毫秒 | 数秒到数小时 |
| **成本** | Token 消耗 | 零 | 人力时间 |

---

## 二、NonAI Agent：让纯代码逻辑成为工作流一等公民

### 2.1 NonAI Agent 的设计理念

LangChain4j Agentic 框架的核心思想是：**任何 Java 类都可以成为 Agent**。你只需要在一个普通 Java 类的方法上添加 `@Agent` 注解，它就可以像 AI Agent 一样参与工作流编排——被顺序、并行、循环或条件分支调用。

```java
// 这不是一个 AI Agent（没有 ChatModel，没有 SystemMessage）
// 但它仍然是一个合法的 Agent，可以被工作流调度！
public class ScoreAggregator {

    @Agent(description = "聚合多个评审为一个综合评审",
           outputKey = "combinedCvReview")
    public CvReview aggregate(
            @V("hrReview") CvReview hr,
            @V("managerReview") CvReview mgr,
            @V("teamMemberReview") CvReview team) {

        double avgScore = (hr.score + mgr.score + team.score) / 3.0;
        String combined = String.join("\n", hr.feedback, mgr.feedback, team.feedback);
        return new CvReview(avgScore, combined);
    }
}
```

**核心优势**：
- 🔄 **无缝互换**：框架对 AI Agent 和 NonAI Agent 一视同仁，使用完全相同的编排 API
- ⚡ **极致性能**：纯 Java 执行，零网络开销，微秒级完成
- 🎯 **结果确定**：同样的输入永远产生同样的输出，无 LLM 的随机性
- 💰 **零 Token 成本**：不调用任何 LLM，完全免费

**NonAI Agent vs @Tool 的选择标准**：

| 场景 | 用 NonAI Agent | 用 @Tool |
|------|:--:|:--:|
| 工作流中的一个固定步骤 | ✅ | — |
| AI Agent 按需决定调用 | — | ✅ |
| 需要从 AgenticScope 读取多个上游输出 | ✅ | — |
| 需要写入 AgenticScope 供下游使用 | ✅ | — |
| 简单的工具函数（查天气、发邮件） | — | ✅ |

> 💡 **简单记忆**：NonAI Agent 是**工作流级别的确定性步骤**，@Tool 是**Agent 级别的可选工具**。

### 2.2 实战一：ScoreAggregator —— 确定性评分聚合

#### 业务场景

在招聘系统的三方评审（参见第21篇并行工作流）中，HR、经理、团队成员分别对候选人进行评分。三者的评审结果是三个独立的 `CvReview` 对象。我们需要将它们**聚合成一个综合评审**。

这个聚合逻辑**完全不需要 LLM**——它只是求平均分 + 合并文本，用 AI 反而可能算错。

#### 完整代码

```java
package com.langchain4j.agentic._08_non_ai_agents;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 非 AI 代理 - 将多个简历评审聚合成一个综合评审
 *
 * 这演示了如何将普通 Java 方法作为一等公民代理在工作流中使用，
 * 使它们可以与 AI 驱动的代理互换使用。
 *
 * 非 AI 代理适用于需要确定性操作的场景，如计算、数据转换和聚合，
 * 在这些场景中你希望避免 LLM 的参与。
 */
public class ScoreAggregator {

    /**
     * 聚合 HR/经理/团队评审为一个综合评审
     *
     * @param hr   HR 评审结果（从 AgenticScope 中 key="hrReview" 读取）
     * @param mgr  经理评审结果（从 AgenticScope 中 key="managerReview" 读取）
     * @param team 团队成员评审结果（从 AgenticScope 中 key="teamMemberReview" 读取）
     * @return 综合评审结果（写入 AgenticScope key="combinedCvReview"）
     */
    @Agent(description = "聚合 HR/经理/团队评审为一个综合评审",
           outputKey = "combinedCvReview")
    public CvReview aggregate(
            @V("hrReview") CvReview hr,
            @V("managerReview") CvReview mgr,
            @V("teamMemberReview") CvReview team) {

        System.out.println("ScoreAggregator 被调用，输入参数：");
        System.out.println("  hrReview: " + hr);
        System.out.println("  managerReview: " + mgr);
        System.out.println("  teamMemberReview: " + team);

        // 计算平均分数（确定性计算，不需要 LLM）
        double avgScore = (hr.score + mgr.score + team.score) / 3.0;

        // 合并所有反馈
        String combinedFeedback = String.join("\n\n",
                "HR 评审：" + hr.feedback,
                "经理评审：" + mgr.feedback,
                "团队成员评审：" + team.feedback
        );

        CvReview result = new CvReview(avgScore, combinedFeedback);
        System.out.println("ScoreAggregator 输出：综合评分 = " + avgScore);
        return result;
    }
}
```

**代码要点解析**：

1. **`@Agent` 注解**：声明这是一个 Agent。`description` 描述其功能（供 Supervisor 理解），`outputKey` 指定输出写入 AgenticScope 的键名
2. **`@V` 参数绑定**：`@V("hrReview")` 表示从 AgenticScope 中读取 key 为 `"hrReview"` 的值——这个 key 必须与上游 Agent 的 `outputKey` 一致
3. **纯 Java 逻辑**：方法体就是普通的 Java 代码，不涉及任何 AI 调用
4. **返回 POJO**：返回的 `CvReview` 对象会自动写入 AgenticScope 的 `combinedCvReview` 键

### 2.3 实战二：StatusUpdate —— 业务规则执行

聚合分数之后，我们需要根据分数阈值**更新数据库中的申请状态**。这也是一个典型的确定性操作。

```java
package com.langchain4j.agentic._08_non_ai_agents;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 非 AI 代理 - 根据综合评审分数更新申请状态
 *
 * 此代理用于确定性业务规则：根据分数阈值决定申请状态。
 * 在实际项目中，这里可以连接数据库、消息队列或外部系统。
 */
public class StatusUpdate {

    /**
     * 根据综合评审分数更新申请状态
     *
     * @param aggregateCvReview 综合简历评审结果
     *        （从 AgenticScope 中 key="combinedCvReview" 读取，
     *         即 ScoreAggregator 的输出）
     */
    @Agent(description = "根据分数更新申请状态")
    public void update(
            @V("combinedCvReview") CvReview aggregateCvReview) {

        double score = aggregateCvReview.score;
        System.out.println("StatusUpdate 被调用，分数：" + score);

        // 根据分数阈值决定申请状态（业务规则）
        if (score >= 8.0) {
            // 实际项目中这里连接真实数据库
            // jdbcTemplate.update("UPDATE applications SET status=? WHERE id=?", "已邀请", appId);
            System.out.println("申请状态已更新为：已邀请");
        } else {
            System.out.println("申请状态已更新为：已拒绝");
        }
    }
}
```

**要点**：
- **Void 返回**：NonAI Agent 可以没有返回值（不写入 AgenticScope），只产生副作用（数据库更新、发送消息等）
- **`@V` 参数名**：`"combinedCvReview"` 与上游 `ScoreAggregator` 的 `outputKey` **完全一致**——这就是 Agent 之间的数据契约
- **生产扩展**：注释中的 `jdbcTemplate.update` 展示了真实场景中的数据库操作

### 2.4 实战三：内联 NonAI —— agentAction() Lambda

有时候你只需要做一个简单的数据转换，不值得单独创建一个类。LangChain4j 提供了 `agentAction()` 方法，让你用 Lambda 表达式定义轻量级 NonAI Agent：

```java
AgenticServices.agentAction(agenticScope -> {
    // 从 AgenticScope 读取上游输出
    CvReview review = (CvReview) agenticScope.readState("combinedCvReview");

    // 做简单转换：0-1 小数 → 0-100 百分制
    agenticScope.writeState("scoreAsPercentage", review.score * 100);

    // 当来自不同系统的 Agent 通信时，经常需要这种输出转换
})
```

**何时用 `agentAction` vs 独立 NonAI 类**：

| 场景 | agentAction() | 独立 NonAI 类 |
|------|:--:|:--:|
| 单行转换逻辑 | ✅ 简洁 | ❌ 过度设计 |
| 需要复用的逻辑 | ❌ 重复代码 | ✅ 封装复用 |
| 多步骤复杂逻辑 | ❌ Lambda 过长 | ✅ 清晰组织 |
| 需要单元测试 | ❌ 难独立测试 | ✅ 可单独测试 |

### 2.5 完整串联：AI 并行 + NonAI 链式处理

现在我们把三个 AI Reviewer（并行执行）和三个 NonAI 步骤（顺序执行）串联成一个完整工作流：

```java
@Test
public void testNonAI() throws Exception {

    // ============ 第一步：构建 AI Agent（并行评审）============
    HrCvReviewer hrReviewer = AgenticServices
            .agentBuilder(HrCvReviewer.class)
            .chatModel(openAiChatModel)
            .outputKey("hrReview")
            .build();

    ManagerCvReviewer managerReviewer = AgenticServices
            .agentBuilder(ManagerCvReviewer.class)
            .chatModel(openAiChatModel)
            .outputKey("managerReview")
            .build();

    TeamMemberCvReviewer teamReviewer = AgenticServices
            .agentBuilder(TeamMemberCvReviewer.class)
            .chatModel(openAiChatModel)
            .outputKey("teamMemberReview")
            .build();

    // 将三个评审员打包成并行工作流
    var executor = Executors.newFixedThreadPool(3);

    UntypedAgent parallelReviewWorkflow = AgenticServices
            .parallelBuilder()
            .subAgents(hrReviewer, managerReviewer, teamReviewer)
            .executor(executor)
            .build();

    // ============ 第二步：构建 NonAI 处理链（顺序执行）============
    UntypedAgent collectFeedback = AgenticServices
            .sequenceBuilder()
            .subAgents(
                    parallelReviewWorkflow,          // ① 并行 AI 评审
                    new ScoreAggregator(),            // ② NonAI：聚合分数
                    new StatusUpdate(),               // ③ NonAI：更新状态
                    AgenticServices.agentAction(scope -> {  // ④ NonAI：格式转换
                        CvReview review = (CvReview) scope
                                .readState("combinedCvReview");
                        scope.writeState("scoreAsPercentage",
                                review.score * 100);
                    })
            )
            .outputKey("scoreAsPercentage")
            .build();

    // ============ 第三步：准备输入数据并执行 ============
    Map<String, Object> arguments = Map.of(
            "candidateCv", loadResource("/documents/tailored_cv.txt"),
            "candidateContact", loadResource("/documents/candidate_contact.txt"),
            "hrRequirements", loadResource("/documents/hr_requirements.txt"),
            "phoneInterviewNotes", loadResource("/documents/phone_interview_notes.txt"),
            "jobDescription", loadResource("/documents/job_description_backend.txt")
    );

    double score = (double) collectFeedback.invoke(arguments);
    executor.shutdown();

    System.out.println("=== 最终分数（百分制）===");
    System.out.println(score);
}
```

**工作流全景图**：

```
用户输入（简历 + 职位描述 + 面试笔记）
  │
  ▼
┌─────────────────────────────────────────────┐
│            ParallelWorkflow                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│  │HrReviewer│ │MgrReviewer│ │TeamReview│    │  ← 3 个 AI Agent
│  │ (GPT-4)  │ │ (GPT-4)   │ │ (GPT-4)  │    │    并行执行
│  └────┬─────┘ └────┬─────┘ └────┬─────┘    │
│       │hrReview    │managerReview│teamMemberReview
└───────┼────────────┼─────────────┼──────────┘
        │            │             │
        ▼            ▼             ▼
┌─────────────────────────────────────────────┐
│           Sequence（顺序执行）                │
│                                             │
│  ScoreAggregator  →  StatusUpdate  →  转换   │  ← 3 个 NonAI Agent
│  (平均分+合并)       (阈值判断)      (×100)    │    零 LLM 调用
└─────────────────────────────────────────────┘
  │
  ▼
最终输出：scoreAsPercentage
```

**成本分析**：

| 步骤 | 类型 | Token 消耗 | 耗时 |
|------|------|-----------|------|
| 三方并行评审 | AI × 3 | ~3000 tokens | ~2 秒 |
| 分数聚合 | NonAI | 0 | < 1ms |
| 状态更新 | NonAI | 0 | < 1ms |
| 格式转换 | NonAI | 0 | < 1ms |
| **合计** | — | **~3000 tokens** | **~2 秒** |

> 💡 **核心原则**：能外包给 NonAI Agent 的步骤越多，你的工作流就会**越快、越准、越便宜**。

---

## 三、HumanInTheLoop：在关键时刻引入人工决策

### 3.1 HumanInTheLoop 的设计理念

**HumanInTheLoop**（人机协同）本质上是一个**特殊的 NonAI Agent**：

- 它不是调用 LLM
- 也不是执行纯 Java 代码
- 而是**暂停工作流**，向人类用户请求输入，拿到输入后再继续执行

```
  ... → HumanInTheLoop → [暂停，等待人工输入] → 恢复执行 → ...
```

**执行流程**：

```
1. 工作流执行到 HumanInTheLoop 节点
2. 调用 responseProvider 回调（展示上下文给人类）
3. 人类阅读信息并做出决策
4. 人类输入通过 responseProvider 返回
5. 返回值写入 AgenticScope（由 outputKey 指定）
6. 工作流从暂停点恢复，继续执行后续步骤
```

**核心 API**：

```java
HumanInTheLoop humanAgent = AgenticServices
        .humanInTheLoopBuilder()
        .description("描述这个人工节点的功能")
        .outputKey("输出键名")
        .responseProvider(scope -> {
            // 读取当前工作流状态
            String aiSuggestion = (String) scope.readState("某个key");
            // 展示给人类
            System.out.println("AI 建议：" + aiSuggestion);
            // 等待并返回人类的输入
            return getUserInput();
        })
        .async(true)   // 可选：异步等待，不阻塞线程
        .build();
```

### 3.2 模式一：简单验证器 —— AI 提议，人类拍板

#### 业务场景

招聘流程的最后一步：AI（HiringDecisionProposer）根据所有评审结果生成一份招聘决策建议，但**最终决定必须由人类 HR 做出**。这是一个典型的"AI 辅助 + 人类决策"模式。

#### 步骤 1：定义 AI 提议者

```java
package com.langchain4j.agentic._09_human_in_the_loop;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 招聘决策提议者 - 总结招聘决策供人工最终验证
 */
public interface HiringDecisionProposer {

    /**
     * 根据评审结果生成招聘决策建议
     * @param cvReview 包含所有参与方反馈的综合评审
     * @return 最多 3 行的招聘原因总结
     */
    @Agent("总结招聘决策供最终验证")
    @SystemMessage("""
        你根据给定的评审结果，在最多 3 行内总结招聘原因，
        供人类做出是否继续的最终决定。
        """)
    @UserMessage("""
        招聘过程中所有相关方的反馈：{{cvReview}}
        """)
    String propose(@V("cvReview") CvReview cvReview);
}
```

#### 步骤 2：构建 HumanInTheLoop 验证器

```java
HumanInTheLoop humanValidator = AgenticServices
        .humanInTheLoopBuilder()
        .description("验证模型提议的招聘决策")
        .outputKey("finalDecision")
        .responseProvider(scope -> {
            // 读取 AI 的建议
            System.out.println("AI 招聘助手建议：" + scope.readState("request"));
            System.out.println("请确认最终决策。");
            System.out.println("选项：邀请现场面试 (I)、拒绝 (R)、保留 (H)");
            System.out.print("> ");

            // 在实际系统中需要输入验证和错误处理
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));
            try {
                return reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException("读取输入失败", e);
            }
        })
        .build();
```

#### 步骤 3：串联为完整工作流

```java
// 将 AI 提议者和人工验证器编排为顺序工作流
UntypedAgent hiringDecisionWorkflow = AgenticServices
        .sequenceBuilder()
        .subAgents(decisionProposer, humanValidator)
        .outputKey("finalDecision")
        .build();

// 执行
Map<String, Object> input = Map.of(
        "cvReview", new CvReview(0.85, """
            技术技能强，除了所需的 React 经验外。
            似乎是一个快速且独立的学习者。文化契合度好。
            工作许可存在潜在问题，但似乎可以解决。
            薪资期望略高于计划预算。
            决定继续进行现场面试。
            """)
);

String finalDecision = (String) hiringDecisionWorkflow.invoke(input);

System.out.println("\n=== 人类的最终决策 ===");
System.out.println("(邀请现场面试 (I)、拒绝 (R)、保留 (H))");
System.out.println(finalDecision);
```

**交互流程**：

```
=== 运行时控制台输出 ===

AI 招聘助手建议：
该候选人技术基础扎实，学习能力强，文化契合度高。
主要风险点：工作许可待确认，薪资略超预算。
综合建议：邀请现场面试。

请确认最终决策。
选项：邀请现场面试 (I)、拒绝 (R)、保留 (H)
> I

=== 人类的最终决策 ===
I
```

**架构图**：

```
┌─────────────────────────────────────────┐
│          SequenceWorkflow               │
│                                         │
│  ┌─────────────────────┐                │
│  │ HiringDecision      │  AI Agent      │
│  │ Proposer (GPT-4)    │  语义总结       │
│  └────────┬────────────┘                │
│           │ outputKey="request"          │
│           ▼                             │
│  ┌─────────────────────┐                │
│  │ HumanInTheLoop      │  Human Agent   │
│  │ 验证器               │  人工决策       │
│  └────────┬────────────┘                │
│           │ outputKey="finalDecision"    │
└───────────┼─────────────────────────────┘
            ▼
    最终决策（I/R/H）
```

### 3.3 模式二：多轮对话机器人 —— 循环直到达成一致

简单验证器适用于**一次性决策**，但有些场景需要 AI 与人类**反复协商**。例如：

> **会议安排**：秘书 AI 提议时间 → 候选人有冲突 → AI 提议另一个时间 → 候选人确认 → 循环结束

这是一个**带 HumanInTheLoop 的循环工作流**。

#### 架构设计

```
┌──────────────────────────────────────────────┐
│              LoopWorkflow                    │
│  maxIterations=5                             │
│                                              │
│  ┌────────────────────────────────────┐      │
│  │         Sequence                   │      │
│  │                                    │      │
│  │  ┌──────────────┐  ┌────────────┐  │      │
│  │  │ Meeting      │  │ HumanInThe │  │      │
│  │  │ Proposer     │→ │ Loop       │  │      │
│  │  │ (AI + 记忆)  │  │ (用户输入)  │  │      │
│  │  └──────────────┘  └────────────┘  │      │
│  │                                    │      │
│  └────────────────┬───────────────────┘      │
│                   │                          │
│                   ▼                          │
│  exitCondition: isDecisionReached(proposal,  │
│                                  answer)?    │
└──────────────────────────────────────────────┘
```

#### 步骤 1：定义带记忆的会议提议 Agent

```java
package com.langchain4j.agentic._09_human_in_the_loop;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface MeetingProposer {

    @Agent("提议会议时间")
    @SystemMessage("""
        你协助 A 公司尝试安排一个关于 {{meetingTopic}} 的新会议。
        为会议预留 3 小时。

        你用一句话向候选人提议一个会议时间段，例如：
        "您下周一上午 10 点有空吗？"
        如果用户有问题，也要回答。

        你的团队有以下会议可用性：下周的周一、周二或周四上午 9 点，
        或者再下一周的周二、周三或周五下午 2 点。
        今天是 {{current_date}}。
        """)
    @UserMessage("""
        之前候选人的回答是：{{candidateAnswer}}
        """)
    String propose(
            @MemoryId String memoryId,
            @V("meetingTopic") String meetingTopic,
            @V("candidateAnswer") String candidateAnswer
    );
}
```

**关键设计**：
- **`@MemoryId`**：让 Agent 记住之前的提议历史，避免每次循环都提议同一个时间
- **`@UserMessage`**：每次循环都将候选人上次的回答作为上下文传入

#### 步骤 2：定义退出条件判断服务

```java
package com.langchain4j.agentic._09_human_in_the_loop;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 决策判断服务 — 用轻量 AI 判断对话是否已达成一致
 */
public interface DecisionsReachedService {

    @SystemMessage("""
        根据交互内容，如果已达成决策则返回 true，
        如果需要进一步讨论以找到解决方案则返回 false。
        """)
    @UserMessage("""
        到目前为止的交互：
          秘书：{{proposal}}
          被邀请人：{{candidateAnswer}}
        """)
    boolean isDecisionReached(
            @V("proposal") String proposal,
            @V("candidateAnswer") String candidateAnswer
    );
}
```

> 💡 **为什么用 AI 判断退出条件？** "达成一致"是一个语义判断，不能简单用字符串匹配。候选人说"好的"、"可以"、"没问题"、"那就这样吧"都是同意的意思。这里可以用一个**便宜的小模型**（如 GPT-4o-mini）来执行。

#### 步骤 3：组装循环工作流

```java
@Test
public void testHumanInTheLoopChatbotWithMemory() throws Exception {

    // 3.1 构建 AI Agent（带记忆）
    MeetingProposer proposer = AgenticServices
            .agentBuilder(MeetingProposer.class)
            .chatModel(openAiChatModel)
            .chatMemoryProvider(memoryId ->
                MessageWindowChatMemory.withMaxMessages(15))
            .outputKey("proposal")
            .build();

    // 3.2 构建退出条件判断器
    DecisionsReachedService decisionService =
            AiServices.create(DecisionsReachedService.class, openAiChatModel);

    // 3.3 构建 HumanInTheLoop
    HumanInTheLoop humanInTheLoop = AgenticServices
            .humanInTheLoopBuilder()
            .description("向用户请求输入的代理")
            .outputKey("candidateAnswer")
            .responseProvider(scope -> {
                System.out.println(scope.readState("proposal"));
                System.out.print("> ");
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(System.in));
                    return reader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException("读取输入失败", e);
                }
            })
            .async(true)  // ⚡ 关键：等待用户输入时不阻塞线程池
            .build();

    // 3.4 将 AI + Human 打包为一个序列（循环的每一轮）
    UntypedAgent agentSequence = AgenticServices
            .sequenceBuilder()
            .subAgents(proposer, humanInTheLoop)
            .output(agenticScope -> Map.of(
                    "proposal", agenticScope.readState("proposal"),
                    "candidateAnswer", agenticScope.readState("candidateAnswer")
            ))
            .outputKey("proposalAndAnswer")
            .build();

    // 3.5 用循环包裹序列
    UntypedAgent schedulingLoop = AgenticServices
            .loopBuilder()
            .subAgents(agentSequence)
            .exitCondition(scope -> {
                System.out.println("--- 检查退出条件 ---");
                String response = (String) scope.readState("candidateAnswer");
                String proposal = (String) scope.readState("proposal");
                return response != null &&
                       decisionService.isDecisionReached(proposal, response);
            })
            .outputKey("proposalAndAnswer")
            .maxIterations(5)  // 安全阀：最多 5 轮
            .build();

    // 3.6 执行
    Map<String, Object> input = Map.of(
            "meetingTopic", "现场访问",
            "candidateAnswer", "hi",    // 初始值，触发第一轮对话
            "memoryId", "user-1234"     // 记忆隔离
    );

    var result = schedulingLoop.invoke(input);
    System.out.println("=== 最终结果 ===");
    System.out.println(result);
}
```

**交互示例**：

```
--- 第 1 轮 ---
您下周一上午 10 点有空吗？
> 周一不行，我有其他安排

--- 检查退出条件 ---（未达成，继续）

--- 第 2 轮 ---
那周二上午 9 点可以吗？
> 周二可以！

--- 检查退出条件 ---（达成一致 ✅，退出循环）

=== 最终结果 ===
{proposal=那周二上午 9 点可以吗？, candidateAnswer=周二可以！}
```

**关键设计要点**：

| 设计点 | 说明 |
|--------|------|
| **ChatMemory** | 让 Agent 记住已提议的时间，避免循环中重复提议 |
| **`async(true)`** | 等待用户输入时不占用工作流线程池 |
| **`maxIterations(5)`** | 安全阀：防止无限循环（用户一直不同意） |
| **`exitCondition`** | 用轻量 AI 判断语义上的"达成一致" |
| **`output()` 方法** | 将序列内两个 Agent 的输出打包为一个复合输出 |

### 3.4 HumanInTheLoop 的变体：GUI 交互模式

上面的示例使用控制台输入。在实际生产环境中，你可以将 `responseProvider` 接入任何 UI 层：

**Swing 弹窗模式**（来自 `langchain4j-spring-boot-12-agentic`）：

```java
HumanInTheLoop humanAgent = AgenticServices
        .humanInTheLoopBuilder()
        .description("向用户询问星座信息")
        .outputKey("sign")
        .responseProvider(scope -> {
            // 使用 Swing 弹窗代替控制台输入
            return JOptionPane.showInputDialog(
                    null,
                    "请输入您的星座：",
                    "信息补充",
                    JOptionPane.QUESTION_MESSAGE
            );
        })
        .build();
```

**Web 应用模式**（生产推荐）：

```java
// responseProvider 不读控制台，而是查询数据库中的待审批任务
HumanInTheLoop webApproval = AgenticServices
        .humanInTheLoopBuilder()
        .description("等待管理者审批")
        .outputKey("approvalResult")
        .responseProvider(scope -> {
            String taskId = (String) scope.readState("approvalTaskId");

            // 轮询数据库，等待人工审批（带超时）
            ApprovalTask task = approvalService
                    .waitForApproval(taskId, Duration.ofHours(24));

            if (task.isApproved()) {
                return "APPROVED:" + task.getComment();
            } else {
                return "REJECTED:" + task.getComment();
            }
        })
        .async(true)
        .build();
```

在这种模式下，前端通过 REST API 或 WebSocket 展示待审批内容，人类操作后将结果写入数据库，`responseProvider` 检测到结果后恢复工作流执行。

---

## 四、混合编排实战：AI + NonAI + Human 三者协同

### 4.1 完整招聘决策工作流

将本章所学全部技术整合到一个端到端的混合执行系统中：

```
                         用户请求
                            │
                            ▼
┌──────────────────────────────────────────────────────────┐
│                   混合执行工作流                           │
│                                                          │
│  ① ParallelWorkflow（AI Agent × 3）                      │
│     HR评审  │  经理评审  │  团队评审                       │
│     ────────┼───────────┼──────────                      │
│             ▼            ▼            ▼                  │
│  ② ScoreAggregator（NonAI Agent）                        │
│     计算平均分 + 合并反馈                                  │
│             │                                            │
│             ▼                                            │
│  ③ StatusUpdate（NonAI Agent）                           │
│     根据分数更新数据库状态                                  │
│             │                                            │
│             ▼                                            │
│  ④ agentAction（NonAI Agent 内联）                        │
│     分数 ×100 格式转换                                     │
│             │                                            │
│             ▼                                            │
│  ⑤ HiringDecisionProposer（AI Agent）                    │
│     生成招聘决策建议                                       │
│             │                                            │
│             ▼                                            │
│  ⑥ HumanInTheLoop（Human Agent）                         │
│     HR 最终确认（邀请/拒绝/保留）                           │
│             │                                            │
│             ▼                                            │
│        最终输出                                           │
└──────────────────────────────────────────────────────────┘
```

**各步骤类型统计**：

| 步骤 | 类型 | 作用 |
|------|------|------|
| ① 并行评审 | AI × 3 | 语义理解：分析简历与职位匹配度 |
| ② 分数聚合 | NonAI | 确定性计算：求平均值 |
| ③ 状态更新 | NonAI | 业务规则：阈值判断 + 数据库写入 |
| ④ 格式转换 | NonAI | 数据适配：小数转百分制 |
| ⑤ 决策提议 | AI | 语义总结：生成人类可读的建议 |
| ⑥ 人工确认 | Human | 最终决策：合规 + 责任归属 |

### 4.2 数据流转与状态管理

在整个混合工作流中，**AgenticScope** 是全局状态总线，所有 Agent 通过它交换数据：

```
Agent 1 (AI)                  Agent 2 (NonAI)
outputKey="hrReview"  ────→  @V("hrReview") 读取
                             outputKey="combinedCvReview" ────→  Agent 3 (NonAI)
                                                                 @V("combinedCvReview") 读取
```

**命名规范建议**：

```java
// ✅ 推荐：用常量统一定义 outputKey，避免拼写错误
public class WorkflowKeys {
    public static final String HR_REVIEW = "hrReview";
    public static final String MANAGER_REVIEW = "managerReview";
    public static final String COMBINED_REVIEW = "combinedCvReview";
    public static final String FINAL_DECISION = "finalDecision";
}

// 在 Agent 定义中使用
@Agent(outputKey = WorkflowKeys.COMBINED_REVIEW)
public CvReview aggregate(
        @V(WorkflowKeys.HR_REVIEW) CvReview hr,
        @V(WorkflowKeys.MANAGER_REVIEW) CvReview mgr) {
    // ...
}
```

### 4.3 异常处理与回滚机制

混合工作流中不同环节的异常处理策略不同：

```java
// NonAI Agent 异常处理
public class StatusUpdate {

    @Agent(description = "根据分数更新申请状态")
    public void update(@V("combinedCvReview") CvReview review) {
        try {
            // 尝试数据库更新
            databaseService.updateStatus(review);
        } catch (DataAccessException e) {
            // 记录失败日志，写入死信队列
            deadLetterQueue.send(new FailedUpdate(review, e));
            throw new AgentExecutionException(
                "状态更新失败，已写入死信队列待人工处理", e);
        }
    }
}

// HumanInTheLoop 超时处理
HumanInTheLoop approvalAgent = AgenticServices
        .humanInTheLoopBuilder()
        .description("管理者审批")
        .outputKey("approvalResult")
        .responseProvider(scope -> {
            // 24 小时超时，超时后自动拒绝
            return approvalService.waitForApproval(
                    taskId, Duration.ofHours(24), "TIMEOUT_REJECT");
        })
        .async(true)
        .build();
```

---

## 五、常见问题与避坑指南

### 5.1 NonAI Agent 没有被调用

**问题现象**：NonAI Agent 的 `@Agent` 方法没有被触发，工作流跳过了它。

**常见原因**：`@V` 参数名与上游 `outputKey` **不匹配**。

```java
// ❌ 错误：参数名不匹配
// 上游 outputKey = "hrReview"
@Agent(outputKey = "combinedCvReview")
public CvReview aggregate(@V("hr_review") CvReview hr) { ... }
//                               ^^^^^^^^^ 不匹配！上游写的是 "hrReview"

// ✅ 正确：参数名必须完全一致
@Agent(outputKey = "combinedCvReview")
public CvReview aggregate(@V("hrReview") CvReview hr) { ... }
```

**解决办法**：使用常量类统一定义所有 key，杜绝拼写差异。

### 5.2 HumanInTheLoop 卡死不返回

**问题现象**：工作流在 HumanInTheLoop 节点挂起，后面的步骤永远不执行。

**原因**：`responseProvider` 阻塞了工作流线程池。

```java
// ❌ 问题代码：同步等待会占用线程池中的线程
HumanInTheLoop human = AgenticServices.humanInTheLoopBuilder()
        // 没有设置 async(true)
        .responseProvider(scope -> {
            Thread.sleep(3600000);  // 等待 1 小时
            return "done";
        })
        .build();
```

**解决办法**：

```java
// ✅ 设置 async(true)，将等待从线程池中剥离
HumanInTheLoop human = AgenticServices.humanInTheLoopBuilder()
        .async(true)  // ← 关键配置
        .responseProvider(scope -> {
            return waitForUserInput();  // 长时间等待不再阻塞线程池
        })
        .build();
```

### 5.3 exitCondition 不触发

**问题现象**：循环工作流中的 `exitCondition` 永远返回 false，工作流卡在死循环。

**根本原因**：只依赖 AI 判断，没有兜底机制。

```java
// ❌ 危险：只依赖 AI 判断
.exitCondition(scope -> decisionService.isDecisionReached(...))

// ✅ 安全：AI 判断 + 硬编码兜底
.exitCondition(scope -> {
    // 硬兜底：超过 5 轮强制退出
    int round = (int) scope.readStateOrDefault("round", 1);
    if (round >= 5) return true;

    // AI 判断
    return decisionService.isDecisionReached(...);
})
.maxIterations(5)  // 第二层兜底：循环级别的硬限制
```

### 5.4 NonAI Agent 返回值类型不兼容

**问题现象**：下游 Agent 读到的数据类型与期望不符。

**解决办法**：用 `agentAction` 做类型适配桥接：

```java
// 上游 NonAI Agent 返回 CvReview，下游期望 Double
AgenticServices.agentAction(scope -> {
    CvReview review = (CvReview) scope.readState("combinedCvReview");
    scope.writeState("score", review.score);  // 提取 double 字段
})
```

### 5.5 常见问题速查表

| 问题现象 | 可能原因 | 解决方案 |
|----------|---------|---------|
| NonAI Agent 未被调用 | `@V` 参数名与 outputKey 不匹配 | 用常量统一定义 key 名 |
| HumanInTheLoop 卡死 | 未设置 async，阻塞线程池 | 设置 `async(true)` |
| 循环不退出 | exitCondition 无兜底 | `maxIterations` + 超时保护 |
| 类型转换失败 | 上下游类型不兼容 | 用 `agentAction` 做适配 |
| 状态 key 冲突 | 多个 Agent 写入同名 key | 用前缀区分，如 `step1_result` |

---

## 六、进阶技巧与最佳实践

### 6.1 NonAI 最大化原则

每设计一个 Agent 步骤时，先问自己三个问题：

```
1. 这个步骤需要语义理解吗？
   → 不需要 → NonAI Agent ✅

2. 这个步骤需要创造性生成吗？
   → 不需要 → NonAI Agent ✅

3. 这个步骤的结果需要 100% 确定吗？
   → 需要 → NonAI Agent ✅
```

**典型适合 NonAI 的场景**：
- 数学计算、统计聚合
- 数据格式转换、字段映射
- 阈值判断、规则引擎
- 数据库 CRUD 操作
- 外部 API 调用（非 LLM）
- 数据校验、格式检查

### 6.2 HumanInTheLoop 粒度控制

只在**真正需要人类判断**的节点介入，避免"审批地狱"：

```
✅ 合适的人工介入节点：
  - 涉及金额的决策（> ¥1000）
  - 法律/合规相关的判断
  - 可能影响用户权益的操作
  - AI 置信度低于阈值的判断

❌ 不应人工介入的节点：
  - 纯数据格式转换
  - 简单的是/否路由
  - 信息查询类操作
  - 批量数据处理
```

### 6.3 混合工作流的测试策略

不同类型的 Agent 需要不同的测试方法：

```java
// NonAI Agent：纯单元测试（无需 Mock LLM，最快）
@Test
public void testScoreAggregator() {
    ScoreAggregator aggregator = new ScoreAggregator();
    CvReview hr = new CvReview(0.9, "优秀");
    CvReview mgr = new CvReview(0.7, "良好");
    CvReview team = new CvReview(0.8, "推荐");

    CvReview result = aggregator.aggregate(hr, mgr, team);

    assertEquals(0.8, result.score, 0.001);  // (0.9+0.7+0.8)/3
}

// HumanInTheLoop：用预设输入模拟
@Test
public void testHumanInTheLoop() {
    HumanInTheLoop human = AgenticServices.humanInTheLoopBuilder()
            .responseProvider(scope -> "I")  // 模拟用户输入
            .build();
    // ... 验证后续流程
}
```

### 6.4 生产环境 HumanInTheLoop 架构

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Agent 工作流 │ ──→ │  审批任务表   │ ←── │  前端审批页面  │
│  (暂停点)     │     │  (MySQL/Redis)│     │  (Web UI)    │
└──────────────┘     └──────────────┘     └──────────────┘
       ↑                                        │
       │          ┌──────────────┐              │
       └──────────│ responseProvider│ ←─────────┘
                  │ (轮询任务状态)   │
                  └──────────────┘
```

关键组件：
1. **审批任务表**：存储待审批任务（taskId、状态、内容、创建时间）
2. **前端审批页**：展示任务内容，收集人类决策
3. **responseProvider**：轮询数据库，检测到决策后返回

### 6.5 成本估算公式

混合执行系统的总成本可以这样估算：

```
总成本 = AI Agent Token 成本 × AI Agent 步数
       + NonAI Agent 成本 × 0
       + Human Agent 人力时间成本 × Human Agent 步数

总延迟 = AI Agent 延迟 × AI Agent 步数（考虑并行）
       + NonAI Agent 延迟 × NonAI Agent 步数
       + 人类等待时间 × Human Agent 步数
```

---

## 结语

本文深入讲解了 LangChain4j Agentic 框架中两种特殊的 Agent 类型——**NonAI Agent** 和 **HumanInTheLoop**。通过完整招聘系统的实战案例，你学会了：

- 如何用 `@Agent` 注解将纯 Java 方法变成工作流的一等公民
- 如何用 `humanInTheLoopBuilder()` 在关键时刻暂停工作流等待人工决策
- 如何设计 AI + NonAI + Human 三者协同的混合执行架构

**混合执行系统的核心价值**：让**AI 做它擅长的**（语义理解、文本生成），**代码做它擅长的**（确定性计算、规则判断），**人类做人类擅长的**（复杂决策、责任归属）。三者各司其职，才能真正构建生产级的 Agent 应用。

下一篇我们将学习 **A2A 协议**——让不同的 Agent 应用之间可以跨进程、跨网络通信与协作。敬请期待！

---

## 延伸阅读

- LangChain4j Agentic 框架官方文档：[Agentic Services](https://docs.langchain4j.dev/tutorials/agentic-services)
- Human-in-the-Loop 设计模式论文
- 《人机协同：AI 系统的安全与伦理设计》

---

### 📋 本文涉及的代码文件

| 文件 | 路径 | 作用 |
|------|------|------|
| `_08_NonAITest.java` | `langchain4j-spring-boot-11-agentic/src/test/java/` | NonAI Agent 混合编排完整测试 |
| `ScoreAggregator.java` | `agentic/_08_non_ai_agents/` | 确定性分数聚合 NonAI Agent |
| `StatusUpdate.java` | `agentic/_08_non_ai_agents/` | 数据库状态更新 NonAI Agent |
| `_09_HumanInTheLoopTest.java` | `langchain4j-spring-boot-11-agentic/src/test/java/` | HumanInTheLoop 两种模式测试 |
| `HiringDecisionProposer.java` | `agentic/_09_human_in_the_loop/` | 招聘决策提议 AI Agent |
| `MeetingProposer.java` | `agentic/_09_human_in_the_loop/` | 会议时间协商 AI Agent |
| `DecisionsReachedService.java` | `agentic/_09_human_in_the_loop/` | 决策达成判断 AiService |
| `_08_HumanInTheLoopTest.java` | `langchain4j-spring-boot-12-agentic/src/test/java/` | HumanInTheLoop + Supervisor 变体 |
| `CvReview.java` | `domain/` | 简历评审领域对象 |

---

> 💡 **提示**：本文代码基于 langchain4j-cookbook 项目的 `langchain4j-spring-boot-11-agentic` 和 `langchain4j-spring-boot-12-agentic` 模块。克隆项目后可直接运行对应测试类，体验混合执行工作流的完整效果。
