# 智能代码审查与优化系统 — LangChain4j Agentic 工作流编排实战

基于 LangChain4j Agentic 框架的代码审查系统，展示 **6 种工作流模式**在真实场景中的有机组合。

## 工作流模式覆盖

| 模式 | 对应组件 | 作用 |
|------|---------|------|
| **Sequential** | CodeParser → IssueIdentifier | 代码解析 → 问题识别 |
| **Non-AI Agent** | StaticAnalyzer | 圈复杂度/行数/命名规范（纯 Java 规则引擎） |
| **Parallel** | SecurityReviewer + PerformanceReviewer + MaintainabilityReviewer | 三维并行审查 |
| **Conditional** | RiskRouter | 高风险→人工确认 / 中风险→自动修复 |
| **Loop** | CodeFixer → ReReviewer | 修复→重审直到质量达标（max 3轮） |
| **Human-in-the-Loop** | 测试模拟人工审批 | 高风险问题暂停等待人工确认 |

## 快速启动

```bash
# 配置 API Key
export DEEPSEEK_API_KEY=your_deepseek_api_key

# 运行集成测试
cd langchain4j-spring-boot-14-code-review-agentic
mvn test -Dtest=CodeReviewWorkflowTest#testFullReviewWorkflow
```

## 示例代码

`src/test/resources/sample-code/` 包含 3 个故意植入问题的 Java 文件：

| 文件 | 植入问题 |
|------|---------|
| `UserService.java` | SQL 注入、N+1 查询、异常吞掉、缺少输入校验 |
| `OrderController.java` | 异常堆栈泄露、Thread.sleep 阻塞、空指针 |
| `PaymentUtil.java` | 硬编码密钥、空指针、缺少事务注解 |

## 项目结构

```
com.langchain4j/
├── Application.java                 # Spring Boot 启动
├── agent/                           # 8 个 AI Agent 接口
│   ├── CodeParser.java              # Seq-1: 解析代码结构
│   ├── IssueIdentifier.java         # Seq-2: 识别潜在问题
│   ├── SecurityReviewer.java        # Par-1: 安全性审查
│   ├── PerformanceReviewer.java     # Par-2: 性能审查
│   ├── MaintainabilityReviewer.java # Par-3: 可维护性审查
│   ├── CodeFixer.java               # Loop-1: 生成修复代码
│   └── ReReviewer.java              # Loop-2: 重新审查
├── nonai/
│   └── StaticAnalyzer.java          # Non-AI Agent: 静态分析
├── domain/                          # 8 个领域模型类
├── util/
│   └── CodeLoader.java              # 加载示例代码
└── config/
    └── AgenticConfig.java           # Agent Bean 配置
```

## 与 Cookbook 其他示例的关系

| 示例 | 本项目如何进阶 |
|------|-------------|
| `11-agentic`（独立模式演示） | 6 种模式在**一个连贯工作流**中组合 |
| `13-customerService`（Agent + Tool） | 展示复杂**工作流编排**而非 Agent 设计 |

## 进阶方向

- **真实 Human-in-the-Loop**：接入审批服务，实现真正的人工审批流程
- **多文件审查**：扩展为同时审查多个文件并交叉引用
- **结构化输出**：Agent 返回 POJO 而非 String，提升类型安全
- **持久化审查历史**：将审查结果存入数据库，追踪代码质量变化趋势
