# 智能代码审查与优化系统 —— 设计规格

> **项目代号**：`langchain4j-spring-boot-14-code-review-agentic`  
> **定位**：Agentic 工作流编排实战教学项目，技术专栏第 32 篇配套代码  
> **日期**：2026-07-22  
> **状态**：设计完成，待进入实现计划

---

## 一、项目概述

### 1.1 一句话定义

智能代码审查系统是一个**教学演示项目**，以 Java 代码审查为业务场景，展示 LangChain4j Agentic 框架中 **Sequential、Parallel、Loop、Conditional、Human-in-the-Loop、Non-AI Agent** 六种工作流模式在一个真实场景中的有机组合。

### 1.2 与现有项目的关系

| 对比维度 | 11-agentic（工作流示例） | 13-customerService（智能客服） | 14-code-review-agentic（本项目） |
|---------|--------------------------|-------------------------------|----------------------------------|
| **模式覆盖** | 9 个独立 Test，每种模式孤立演示 | 单一 Agent + Tool Calling | 6 种模式组合在一个连贯工作流中 |
| **交互方式** | JUnit 测试 + println | Web 应用 + SSE | JUnit 测试 + 日志输出 |
| **教学重点** | "这个模式怎么写" | "Agent 设计范式" | "多个模式怎么组合" |
| **复杂度** | 简单（每模式 1 个 Agent） | 中等（15 类） | 中等（20 类） |

### 1.3 目标读者

已掌握单种工作流模式语法的 Java 开发者，需要学习如何将多种模式**组合**为一个完整的业务工作流。

---

## 二、工作流架构

```
sample-code/*.java（测试资源中的故意有问题的代码）
    │
    ▼
┌─ Sequential ──────────────────────────────────────────────────┐
│  CodeParser          → 解析代码结构（类/方法/字段/依赖）        │
│       ↓                                                        │
│  IssueIdentifier     → 识别潜在问题，生成 Issue 清单            │
│  输出: List<CodeIssue>                                         │
└────────────────────────────────────────────────────────────────┘
    │
    ├──→ ┌─ Non-AI Agent ──────────────────────────────────────┐
    │    │  StaticAnalyzer → 圈复杂度 / 命名规范 / 代码行数      │
    │    │  输出: StaticAnalysisResult                          │
    │    └──────────────────────────────────────────────────────┘
    │
    ▼
┌─ Parallel ──────────────────────────────────────────────────────┐
│  SecurityReviewer        → SQL 注入 / XSS / 敏感信息暴露         │
│  PerformanceReviewer     → N+1 查询 / 内存泄漏 / 死锁风险        │
│  MaintainabilityReviewer → 命名 / 注释 / 耦合度 / SOLID          │
│  输出: CombinedReviewResult（3 个 Agent 结果聚合）               │
└──────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─ Conditional ───────────────────────────────────────────────────┐
│  RiskRouter:                                                    │
│  ├── 高风险（安全问题）→ Human-in-the-Loop 人工确认               │
│  ├── 中风险（性能/可维护性）→ Loop 自动修复                       │
│  └── 低风险（代码风格等）→ 直接进入最终报告                       │
└──────────────────────────────────────────────────────────────────┘
    │
    ├── 高风险 → ┌─ Human-in-the-Loop ─────────────────────────┐
    │            │  工作流暂停，等待人工审批                       │
    │            │  "SecurityReviewer 发现 SQL 注入风险，确认修复？"│
    │            └───────────────────────────────────────────────┘
    │
    ▼ 中风险
┌─ Loop ──────────────────────────────────────────────────────────┐
│  CodeFixer   → 根据审查意见生成修复代码                          │
│       ↓                                                        │
│  ReReviewer  → 重新审查修复后代码                                │
│  退出条件: qualityScore >= 0.8  或  maxIterations = 3           │
│  输出: FixedCode + FinalReviewReport                           │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
最终输出：聚合的审查报告
  · Non-AI 静态分析结果（圈复杂度、命名规范等硬性指标）
  · AI 三维审查意见（安全性、性能、可维护性）
  · 修复代码（如有自动修复）
  · 最终质量评分
```

### 2.1 六种模式的使用理由

| 模式 | 位置 | 为什么这里必须用这个模式 |
|------|------|------------------------|
| **Sequential** | 代码解析 → 问题识别 | 问题识别依赖代码解析的结果——严格先后依赖 |
| **Non-AI Agent** | 静态分析 | 圈复杂度、命名规范是确定性规则，不需要 LLM，用规则引擎即可 |
| **Parallel** | 三维审查 | 安全性/性能/可维护性互不依赖，并行可将审查时间从 ~9s 降至 ~3s |
| **Conditional** | 风险路由 | 不同风险等级的处理路径天然不同——不能一刀切 |
| **Loop** | 修复 → 重审 | 修复不一定一次到位，上限 3 轮防止死循环 |
| **Human-in-the-Loop** | 高风险审批 | 安全漏洞涉及敏感操作，修复前必须人工确认 |

---

## 三、项目结构

### 3.1 包与类清单（20 个类）

```
com.langchain4j/
├── Application.java                     # Spring Boot 启动
├── agent/
│   ├── CodeParser.java                  # Seq-1: 解析代码结构
│   ├── IssueIdentifier.java             # Seq-2: 识别潜在问题
│   ├── SecurityReviewer.java            # Par-1: 安全性审查
│   ├── PerformanceReviewer.java         # Par-2: 性能审查
│   ├── MaintainabilityReviewer.java     # Par-3: 可维护性审查
│   ├── CodeFixer.java                   # Loop-1: 生成修复代码
│   ├── ReReviewer.java                  # Loop-2: 重新审查
│   └── RiskRouter.java                  # Cond: 风险等级路由
├── nonai/
│   └── StaticAnalyzer.java              # Non-AI: 静态分析（纯 Java 规则引擎）
├── domain/
│   ├── CodeIssue.java                   # 代码问题（类型/严重程度/行号/描述）
│   ├── StaticAnalysisResult.java        # 静态分析结果
│   ├── SecurityReview.java              # 安全性审查结果
│   ├── PerformanceReview.java           # 性能审查结果
│   ├── MaintainabilityReview.java       # 可维护性审查结果
│   ├── CombinedReviewResult.java        # 并行审查聚合 + 质量评分
│   ├── FixResult.java                   # 修复结果（修复后的代码）
│   └── FinalReport.java                 # 最终审查报告
├── util/
│   └── CodeLoader.java                  # 从 classpath 加载示例代码
└── config/
    └── AgenticConfig.java               # ChatModel Bean + Agent 实例化

src/test/java/com/langchain4j/
└── CodeReviewWorkflowTest.java          # 集成测试：端到端工作流演示

src/test/resources/
├── application.properties               # 测试环境配置
└── sample-code/
    ├── UserService.java                 # SQL 注入 + N+1 查询 + 缺少校验
    ├── OrderController.java             # 异常堆栈泄露 + Thread.sleep
    └── PaymentUtil.java                 # 硬编码密钥 + 空指针 + 缺少事务
```

### 3.2 类职责速查

| 类 | 类型 | 职责 | 行数估算 |
|---|------|------|---------|
| `Application` | 启动 | Spring Boot 入口 | ~10 |
| `CodeParser` | AI Agent | 解析代码结构 | ~15 |
| `IssueIdentifier` | AI Agent | 识别代码问题 | ~15 |
| `SecurityReviewer` | AI Agent | 安全性维度审查 | ~15 |
| `PerformanceReviewer` | AI Agent | 性能维度审查 | ~15 |
| `MaintainabilityReviewer` | AI Agent | 可维护性维度审查 | ~15 |
| `CodeFixer` | AI Agent | 生成修复代码 | ~15 |
| `ReReviewer` | AI Agent | 修复后重新审查 | ~15 |
| `RiskRouter` | 决策逻辑 | 风险等级判定 | ~30 |
| `StaticAnalyzer` | Non-AI | 规则引擎静态分析 | ~60 |
| `CodeIssue` | Domain | 问题描述 | ~30 |
| `StaticAnalysisResult` | Domain | 静态分析结果 | ~30 |
| `SecurityReview` | Domain | 安全性审查结果 | ~30 |
| `PerformanceReview` | Domain | 性能审查结果 | ~30 |
| `MaintainabilityReview` | Domain | 可维护性审查结果 | ~30 |
| `CombinedReviewResult` | Domain | 审查聚合 + 评分计算 | ~40 |
| `FixResult` | Domain | 修复结果 | ~20 |
| `FinalReport` | Domain | 最终报告 | ~40 |
| `CodeLoader` | Util | 加载示例代码 | ~20 |
| `AgenticConfig` | Config | Bean 配置 | ~30 |
| **总计** | | | **~550 行** |

---

## 四、Domain 模型设计

### 4.1 CodeIssue
```java
@Data @Builder
public class CodeIssue {
    String type;        // SECURITY / PERFORMANCE / MAINTAINABILITY / STYLE
    String severity;    // HIGH / MEDIUM / LOW
    Integer lineNumber; // 问题所在行号
    String filePath;    // 问题所在文件
    String title;       // 问题标题，如 "SQL 注入风险"
    String description; // 问题描述，如 "第 23 行使用字符串拼接构建 SQL"
    String suggestion;  // 修复建议
}
```

### 4.2 CombinedReviewResult（并行审查聚合结果）
```java
@Data @Builder
public class CombinedReviewResult {
    SecurityReview securityReview;
    PerformanceReview performanceReview;
    MaintainabilityReview maintainabilityReview;
    Double qualityScore;          // 综合质量评分 0.0-1.0
    String riskLevel;             // HIGH / MEDIUM / LOW
    List<CodeIssue> allIssues;    // 所有三维问题合并列表
}
```

### 4.3 FinalReport
```java
@Data @Builder
public class FinalReport {
    String sourceFileName;
    StaticAnalysisResult staticAnalysis;   // Non-AI 分析
    CombinedReviewResult aiReview;         // AI 审查
    FixResult fixResult;                   // 修复结果（如有）
    Double finalScore;
    String summary;                        // AI 生成的总结
}
```

---

## 五、测试驱动的交互设计

### 5.1 测试入口

```java
@SpringBootTest
public class CodeReviewWorkflowTest {

    @Autowired private OpenAiChatModel chatModel;

    @Test
    public void testFullReviewWorkflow() {
        // 1. 加载示例代码
        String userServiceCode = CodeLoader.load("sample-code/UserService.java");
        System.out.println("=== 原始代码 ===");
        System.out.println(userServiceCode);

        // 2. 构建 Agents
        // 3. 编排工作流
        // 4. 执行工作流
        // 5. 输出最终报告
    }
}
```

### 5.2 输出示例（测试运行时的日志）

```
=== 审查开始：UserService.java ===

[Sequential] CodeParser → 解析完成：3 个方法，2 个依赖注入
[Sequential] IssueIdentifier → 发现 5 个潜在问题

[Non-AI Agent] StaticAnalyzer → 圈复杂度 12（警告），方法行数 45（警告）

[Parallel] SecurityReviewer    → 发现 SQL 注入风险（HIGH）
[Parallel] PerformanceReviewer → 发现 N+1 查询（MEDIUM）
[Parallel] MaintainabilityReviewer → 发现缺少注释、方法过长（LOW）

[Conditional] RiskRouter → 安全风险 HIGH，进入 Human-in-the-Loop
[Human-in-the-Loop] 请确认：在第 23 行修复 SQL 注入风险？(y/n)
  → [模拟] 用户批准

[Loop] Round 1: CodeFixer → ReReviewer → qualityScore: 0.65 (< 0.8，继续)
[Loop] Round 2: CodeFixer → ReReviewer → qualityScore: 0.85 (>= 0.8，退出)

=== 最终报告 ===
文件: UserService.java
静态分析: 圈复杂度 12 ⚠️ / 方法行数 45 ⚠️
安全性: 2 个问题（1 个已修复，1 个人工确认）
性能: 1 个问题（1 个已修复）
可维护性: 2 个问题（2 个已修复）
最终评分: 0.85 / 1.0
```

---

## 六、示例代码设计（故意植入的问题）

### 6.1 UserService.java

| 行 | 问题 | 类型 | 严重度 |
|----|------|------|--------|
| 23 | `"SELECT * FROM users WHERE name='" + name + "'"` | SQL 注入 | HIGH |
| 27 | `for (Order o : orders) { dao.findItems(o.getId()); }` | N+1 查询 | MEDIUM |
| 35 | `public void updateUser(String name, String email)` — 无输入校验 | 安全风险 | MEDIUM |
| 42 | `catch (Exception e) { }` — 吞掉异常 | 可维护性 | MEDIUM |

### 6.2 OrderController.java

| 行 | 问题 | 类型 | 严重度 |
|----|------|------|--------|
| 18 | `return ResponseEntity.ok(e.getMessage())` — 异常堆栈返回前端 | 信息泄露 | HIGH |
| 25 | `Thread.sleep(5000)` — 阻塞线程 | 性能 | MEDIUM |
| 30 | `@GetMapping("/order")` — 方法 80 行 | 可维护性 | LOW |
| 33 | `String status = null; if (status.equals("PAID"))` — 空指针 | Bug | MEDIUM |

### 6.3 PaymentUtil.java

| 行 | 问题 | 类型 | 严重度 |
|----|------|------|--------|
| 8 | `private static final String SECRET = "sk-abc123"` | 硬编码密钥 | HIGH |
| 15 | `BigDecimal amount` 未判 null 即 `.multiply()` | 空指针 | MEDIUM |
| 22 | `@Transactional` 缺失但执行多表写操作 | 数据一致性 | MEDIUM |

---

## 七、技术栈与依赖

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.4.2 |
| AI 框架 | LangChain4j BOM | 1.14.0 |
| 模型 | DeepSeek-chat（OpenAI 兼容协议） | — |
| 构建 | Maven | 3.6+ |
| JDK | Java | 17+ |
| 测试 | JUnit 5 + spring-boot-starter-test | — |

Maven 依赖（最小集合）：
- `langchain4j-spring-boot-starter`
- `langchain4j-open-ai-spring-boot-starter`
- `spring-boot-starter-test`
- `lombok`

---

## 八、设计决策记录

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| 交互方式 | Test / Web | Test | 聚焦工作流编排，无前端干扰 |
| 代码来源 | 资源文件 / Git diff / 本地扫描 | 资源文件 | 可复现、可精心设计问题 |
| 模式覆盖 | 6 种 / 4 种 | 6 种 | 全面展示工作流组合 |
| Human-in-the-Loop 实现 | 真正交互 / 测试模拟 | 测试模拟 | 教学用测试默认批准，文档中说明生产做法 |

---

## 九、不与现有内容重复

| 与 11-agentic 的区别 | 与 13-customerService 的区别 |
|---------------------|---------------------------|
| 11-agentic 每个 Test 独立演示一种模式 | 13-customerService 只用了单一 Agent + Tool Calling |
| 本项目展示 6 种模式如何组合成**一个**完整工作流 | 本项目展示复杂工作流编排而非 Agent 设计范式 |
| 强调"Workflow Composition"而非"Workflow Basics" | 强调多 Agent 协作而非 Agent + Tool |

---

## 十、进度

- [x] 设计规格批准
- [ ] 实现计划（writing-plans）
- [ ] 代码实现
- [ ] 文章撰写

---

**文档版本**：v1.0  
**最后更新**：2026-07-22
