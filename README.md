# langchain4j-cookbook

《基于 LangChain4j 的 Java AI 应用开发实战大全》—— 面向 Java 开发者的大模型应用工程化完全指南。内容系统覆盖 LangChain4j 核心架构与实战全链路：从 ChatModel / EmbeddingModel 多厂商接入（OpenAI、DeepSeek、Ollama、阿里百炼等），到声明式 AI Service 接口设计；从 RAG 检索增强生成的文档解析、向量化、向量库（Chroma/Qdrant/Milvus/Pinecone）集成，到 Tools / Function Calling 工具编排与 ReAct Agent 智能体构建；涵盖 ChatMemory 记忆管理、结构化输出（POJO / JSON）、TokenStream 流式响应、图像多模态处理等企业级特性。全书以 Spring Boot 工程为底座，通过智能客服、企业知识库、AI 编程助手、数据分析 Agent 等生产级项目，手把手带你掌握 Java 生态大模型应用从原型设计到上线部署的完整技术栈与最佳实践。

---

## 技术栈

| 类别 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.4.2 |
| LangChain4j | 1.14.0 |
| Maven | 3.6+ |

---

## 项目结构

本项目包含 **32 个子模块**，分为两大类：**独立 Java 示例**（纯 main 方法，无 Spring 依赖）和 **Spring Boot 示例**（基于 langchain4j-spring-boot-starter 构建的完整 Web 应用）。

---

### 第一部分：独立 Java 示例

#### langchain4j-01-basic-examples — 基础功能示例

LangChain4j 核心 API 快速入门，通过 13 个独立的 main 方法示例覆盖最常用的功能点：

| 示例 | 说明 |
|------|------|
| T01_ChatModelExamples | 最简对话：使用 OpenAI ChatModel 发送消息并获取回复 |
| T02_ModelParametersExamples | 模型参数调优：temperature、topP、maxTokens 等 |
| T03_ChatModeWithImageExamples | 多模态：图片输入 + 文本描述的视觉理解 |
| T04_PromptTemplateExample | 提示词模板：变量占位与动态填充 |
| T04_StructuredPromptTemplateExample | 结构化提示词模板 |
| T05_StreamingExamples | 流式输出：逐 Token 实时返回 |
| T06_ChatMemoryExamples | 对话记忆：多轮对话上下文保持 |
| T07_FewShotExamples | Few-Shot 少样本提示 |
| T08_EmbeddingModelTextClassifierExample | 文本分类：基于嵌入向量的语义分类 |
| T09_InProcessEmbeddingModelExamples | 本地嵌入模型（无需网络） |
| T09_HuggingFaceEmbeddingModelExample | HuggingFace 嵌入模型接入 |
| T10_OpenAiImageModelExamples | 图像生成模型调用 |
| T10_OpenAiAudioModelExamples | 音频模型：语音转文字等 |

---

#### langchain4j-02-ai-service-examples — AI Service 声明式服务

讲解 LangChain4j 的核心编程范式——AI Service 接口。无需手写实现，定义接口即可获得 AI 能力：

| 示例 | 说明 |
|------|------|
| AIServiceExamples | 基础 AI Service：@SystemMessage、@UserMessage 注解使用 |
| AIServiceExtractingExamples | 信息提取：从文本中提取结构化数据（如日期、金额） |
| AIServiceExtractingPOJOExamples | 提取为 POJO：将非结构化文本转换为强类型 Java 对象 |
| ServiceWithMemoryExample | 带记忆的对话服务 |
| ServiceWithMemoryForEachUserExample | 多用户记忆隔离 |
| ServiceWithPersistentMemoryExample | 持久化记忆（重启不丢失） |
| ServiceWithPersistentMemoryForEachUserExample | 多用户持久化记忆 |
| ServiceWithToolsExample | AI Service 集成工具调用（Function Calling） |
| ServiceWithDynamicToolsExample | 动态工具：运行时决定可用工具集 |

---

#### langchain4j-03-embedding-store-memory — 内存向量存储

最简单的向量存储入门示例。不依赖任何外部服务，适合本地学习和快速原型验证。

---

#### langchain4j-03-embedding-store-chroma — Chroma 向量存储

集成开源向量数据库 Chroma。展示 Document 的嵌入存储与相似度检索，适合中小规模 RAG 场景。

---

#### langchain4j-03-embedding-store-qdrant — Qdrant 向量存储

接入 Qdrant 向量数据库。Java 官方支持完善、部署简单、性能足够大多数业务场景，是生产环境的推荐选择。

---

#### langchain4j-03-embedding-store-milvus — Milvus 向量存储

接入 Milvus 向量数据库。适用于数据量达到亿级且团队有运维能力的场景。

---

#### langchain4j-04-mcp-examples — MCP 工具集成

讲解 MCP（Model Context Protocol）协议的工具集成方式：

| 示例 | 说明 |
|------|------|
| McpToolsExampleOverStdio | 通过标准输入/输出与 MCP 工具通信 |
| McpToolsExampleOverHttp | 通过 HTTP 协议与 MCP 工具通信 |
| Bot | 基于 MCP 工具的对话机器人 |

---

#### langchain4j-05-rag-examples — RAG 检索增强生成

从入门到进阶，系统覆盖 RAG 各种模式。按学习路径组织（`_01` → `_03`）：

| 示例 | 说明 |
|------|------|
| Easy_RAG_Example | 最简单 RAG：3 行代码实现文档问答 |
| Naive_RAG_Example | 朴素 RAG：手写检索+生成全流程 |
| Advanced_RAG_with_Query_Compression | 查询压缩：压缩对话上下文提升检索精度 |
| Advanced_RAG_with_Query_Routing | 查询路由：按意图分发到不同检索器 |
| Advanced_RAG_with_ReRanking | 重排序：对召回结果二次排序提升相关性 |
| Advanced_RAG_with_Metadata | 元数据增强：利用文档元数据改善回答质量 |
| Advanced_RAG_with_Metadata_Filtering | 元数据过滤：检索时按标签/来源等过滤 |
| Advanced_RAG_Skip_Retrieval | 跳过检索：模型自主判断是否需要检索 |
| Advanced_RAG_Multiple_Retrievers | 多检索器：融合多个检索源的结果 |
| Advanced_RAG_Web_Search_Example | 网络搜索：检索时实时查询互联网 |
| Advanced_RAG_Return_Sources | 返回来源：向用户展示回答的文档依据 |
| Advanced_RAG_SQL_Database_Retreiver | SQL 检索器：自然语言转 SQL 查询数据库 |

---

#### langchain4j-06-ollama-examples — Ollama 本地模型

使用 Testcontainers 自动启动 Ollama 容器，演示本地模型的对话和流式调用：

| 示例 | 说明 |
|------|------|
| OllamaChatModelTest | 本地模型的同步对话调用 |
| OllamaStreamingChatModelTest | 本地模型的流式输出 |

---

### 第二部分：Spring Boot 示例

#### langchain4j-spring-boot-01-openai-helloWorld — HelloWorld

Spring Boot 集成 LangChain4j 的最简入门。一个 Controller 直接注入 ChatModel，通过 REST API 与 AI 对话。运行后访问 `http://localhost:8082` 即可体验。

---

#### langchain4j-spring-boot-02-aiService — 声明式 AI 服务

同一个 Controller 展示了三种调用方式的对比：

| 端点 | 方式 | 特点 |
|------|------|------|
| /model1 | 直接调用 ChatModel | 底层 API，灵活但需手写逻辑 |
| /model2 | AiServices.create() 手动代理 | 接口化编程，需手动创建 |
| /model3 | @AiService 自动代理 | 声明式注解，Spring 自动管理（推荐） |

---

#### langchain4j-spring-boot-02-deepSeek-examples — DeepSeek 模型接入

接入 DeepSeek 大模型的完整配置示例，包括 application.properties 中的 API Key、Base URL、模型名称配置。

---

#### langchain4j-spring-boot-02-dashscope-examples — 阿里百炼接入

通过阿里百炼 DashScope 平台接入通义千问等国内大模型，展示 langchain4j-community-dashscope 启动器的使用。

---

#### langchain4j-spring-boot-02-multi-model-examples — 多模型集成

同时接入多个大模型厂商（OpenAI + DeepSeek + 阿里百炼），展示如何在同一个 Spring Boot 应用中配置和切换不同的 ChatModel。

---

#### langchain4j-spring-boot-03-memory — 对话记忆

演示 ChatMemory 的使用：在多轮对话中自动携带历史上下文，让 AI 记住之前的对话内容。

---

#### langchain4j-spring-boot-03-memoryEachUser — 用户隔离对话记忆

在 ChatMemory 的基础上，通过 @MemoryId 注解按用户 ID 隔离对话记忆。每个用户拥有独立的对话上下文，互不干扰。

---

#### langchain4j-spring-boot-03-streaming — 流式输出

使用 TokenStream / Flux\<String\> 实现 SSE（Server-Sent Events）流式输出。逐 Token 实时返回 AI 回复，提升用户交互体验。

---

#### langchain4j-spring-boot-03-listener — AI 服务监听器

注册模型请求/响应监听器，实现日志记录、性能监控、请求拦截等横切关注点。

---

#### langchain4j-spring-boot-04-inMemoryStore — 内存持久化存储

将对话记忆存入内存中的 EmbeddingStore，重启后数据丢失。适合开发调试阶段。

---

#### langchain4j-spring-boot-04-inMysqlStore — MySQL 持久化存储

将对话记忆存入 MySQL 数据库，实现持久化存储。包含完整的 JPA + MySQL 配置示例，重启不丢失对话历史。

---

#### langchain4j-spring-boot-05-prompt — 系统提示词

演示 @SystemMessage 注解的使用。通过系统提示词设定 AI 的角色、语气、回答范围等行为约束。

---

#### langchain4j-spring-boot-05-structured-output — 结构化输出

将 AI 的非结构化自然语言回复转换为强类型的 Java POJO 或 JSON。适用于信息提取、表单填充等场景。

---

#### langchain4j-spring-boot-06-tools — 工具调用

演示 @Tool 注解的使用：将 Java 方法暴露给大模型调用。模型可以自主决策何时调用工具、传什么参数，实现 Function Calling 能力。

---

#### langchain4j-spring-boot-07-lostFoundAssistant — 失物招领助手

**第一个生产级实战项目。** 基于 Spring Boot + MySQL + JPA + Thymeleaf 的完整全栈应用：

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.4.2 + Spring Data JPA |
| 数据库 | MySQL 8.0 |
| 前端 | HTML5 + CSS3 + JavaScript + Thymeleaf 模板 |
| 功能 | 失物登记、按名称/地点搜索、CRUD 完整接口、响应式页面 |

---

#### langchain4j-spring-boot-08-rag — RAG 检索增强生成

Spring Boot 环境下的 RAG 实战。集成 Apache Tika 文档解析（支持 PDF + Office）、Pinecone 向量库、Easy RAG 框架。完整演示文档导入 → 向量化 → 检索 → 增强生成的闭环。

---

#### langchain4j-spring-boot-08-rag-AIBot — 基于 RAG 的智能客服

将 RAG 能力封装为智能客服机器人。结合自定义文档知识库，让 AI 能回答企业专属问题，而非依赖通用训练数据。

---

#### langchain4j-spring-boot-09-lostFoundAssistant — 失物招领助手 + RAG

在之前失物招领 CRUD 应用的基础上，引入 RAG 增强能力。让助手能基于历史失物数据提供更智能的搜索和匹配建议。

---

#### langchain4j-spring-boot-10-mcp — MCP 协议实践

Spring Boot 环境下集成 MCP（Model Context Protocol）。演示如何通过 MCP 协议让 AI 模型安全地访问外部工具和数据源。

---

#### langchain4j-spring-boot-11-agentic — Agentic 智能体工作流（一）

系统讲解 LangChain4j Agentic 框架的核心工作流模式，以"智能招聘系统"为主线串联：

| 工作流 | 示例 | 说明 |
|--------|------|------|
| 01_basic | CvGenerator | 最简 Agent：单步完成简历生成 |
| 02_sequential | CvTailor / SequenceCvGenerator | 顺序工作流：简历生成 → 简历优化，串行执行 |
| 03_loop | CvReviewer / ScoredCvTailor | 循环工作流：审查 → 修改 → 再审查，直到质量达标 |
| 04_parallel | HR/Tech/Manager 三方评审 | 并行工作流：三人同时评审，最后汇总意见 |
| 05_conditional | InterviewOrganizer / EmailAssistant | 条件分支：根据条件决定下一步走向 |
| 06_composed | CandidateWorkflow / HiringTeamWorkflow | 组合工作流：嵌套多种模式构建复杂流程 |
| 07_supervisor | HiringSupervisor | 主管编排：一个主 Agent 调度多个子 Agent |
| 08_non_ai_agents | ScoreAggregator / StatusUpdate | 非 AI Agent：纯代码逻辑节点参与工作流 |
| 09_human_in_the_loop | HiringDecisionProposer / DecisionsReachedService | 人机协同：工作流中断等待人工审批 |

---

#### langchain4j-spring-boot-12-agentic — Agentic 智能体工作流（二）

在 11 的基础上进一步拓展，覆盖更多实战场景：

| 工作流 | 示例 | 说明 |
|--------|------|------|
| 01_basic | CustomerServiceAssistant | 客服智能体：基础的多轮对话客服 |
| 02_sequential | RequirementAnalyst → SolutionDesigner → QualityReviewer | 需求分析 → 方案设计 → 质检，三阶段流水线 |
| 03_loop | CodeReviewer / CodeOptimizer | 代码审查循环：审查 → 优化 → 再审查 |
| 04_parallel | FoodExpert / MovieExpert / EveningPlannerAgent | 晚间计划：美食推荐 + 电影推荐并行，汇总输出 |
| 05_conditional | CategoryRouter → HR/Tech/Finance Expert | 智能路由：按问题类别自动分发到对应专家 |
| 06_AIAgent | WithdrawAgent / CreditAgent / ExchangeAgent | 银行 AI Agent：取款、贷款、换汇三业务智能体 |
| 07_NonAIAgent | ExchangeOperator | 非 AI 换汇操作员：纯计算逻辑的 Agent 节点 |
| 08_HumanInTheLoop | AstrologyAgent | 人机协同：运势分析需人工审核确认 |
| 09_A2A | A2ACreativeWriter / StoryCreator / StoryStyleEditor | Agent-to-Agent 通信：Agent 之间直接协作 |

---

#### langchain4j-spring-boot-12-agentic-a2aService-provider — A2A 服务提供方

完整的 A2A（Agent-to-Agent）协议服务端实现：

| 组件 | 说明 |
|------|------|
| AgentCard | Agent 元数据描述（名称、能力、技能列表），通过 `/.well-known/agent-card.json` 暴露 |
| A2AJsonRpcController | JSON-RPC 2.0 协议控制器，支持 tasks/send、tasks/get、tasks/cancel、agent/getCard |
| StoryWriterService | AI 故事创作服务——A2A Agent 的实际执行能力 |
| 会话管理 | 基于 ConcurrentHashMap 的任务状态追踪（演示用途） |

该模块与 `langchain4j-spring-boot-12-agentic` 中的 `_09_A2A` 配合使用：12-agentic 作为 A2A 客户端发起请求，a2aService-provider 作为服务端响应。

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- （部分模块需要）MySQL 8.0、Docker

### 运行方式

```bash
# 1. 克隆仓库
git clone <repo-url>
cd langchain4j-cookbook

# 2. 运行某个 Spring Boot 模块（以 HelloWorld 为例）
mvn -pl langchain4j-spring-boot-01-openai-helloWorld spring-boot:run

# 3. 访问 http://localhost:8082

# 4. 运行独立 Java 示例
mvn -pl langchain4j-01-basic-examples exec:java -Dexec.mainClass="com.langchain4j.T01_ChatModelExamples"
```

### 关于 API Key

示例默认使用 LangChain4j 官方提供的免费演示端点：

```properties
langchain4j.open-ai.chat-model.api-key=demo
langchain4j.open-ai.chat-model.base-url=http://langchain4j.dev/demo/openai/v1
langchain4j.open-ai.chat-model.model-name=gpt-4o-mini
```

如需接入真实 API，请在各模块的 `application.properties` 中替换为你的 API Key 和 Base URL。
