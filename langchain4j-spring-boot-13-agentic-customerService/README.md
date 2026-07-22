# 电商售后智能客服 -- LangChain4j 多 Agent + RAG 实战

基于 LangChain4j 的电商售后智能客服系统，展示多 Agent 协作、RAG 检索增强生成、ChatMemory 对话记忆的正确用法。

## 项目定位

本项目是 LangChain4j Cookbook 系列的第 13 个示例，聚焦三个核心能力的实战教学：

| 能力 | 展示方式 | Cookbook 前置知识 |
|------|---------|------------------|
| **多 Agent / Tool Calling** | 1 个 @AiService + 2 组 @Tool，LLM 自主路由 | 建议先看 `06-tools` |
| **RAG 检索增强生成** | 真实政策文档 Embedding 向量检索 | 建议先看 `08-rag` |
| **ChatMemory 对话记忆** | ChatMemoryProvider + @MemoryId，多轮对话自动关联 | 建议先看 `03-memoryEachUser` |

与更简单示例的区别：本项目将三个能力整合到一个真实业务场景中，展示它们如何协同工作。

## 业务场景

三个核心子场景，覆盖电商售后最常见的用户需求：

- **退换货咨询与申请**：查退货政策 验证订单 创建退货单（Tool Calling + RAG）
- **物流查询**：查退货进度 查物流轨迹（Tool Calling 链式调用）
- **售后政策问答**：退换货条件、保修范围、运费规则（RAG 检索）

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 2. 创建数据库

在 MySQL 中执行 `src/main/resources/schema.sql`，或手动创建：

```sql
CREATE DATABASE customer_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 配置 API Key

设置两个环境变量：

```bash
# DeepSeek API Key（ChatModel + StreamingChatModel）
export DEEPSEEK_API_KEY=your_deepseek_api_key

# 阿里 DashScope API Key（EmbeddingModel）
export DASHSCOPE_API_KEY=your_dashscope_api_key
```

### 4. 运行

```bash
mvn spring-boot:run
```

应用启动时自动加载 `src/main/resources/policies/` 目录下的政策文档并构建 RAG 索引。

### 5. 打开浏览器

访问 http://localhost:8082

试试这些对话：

- "我刚买的蓝牙耳机有杂音，能退吗？"
- "退货的运费谁出？"
- "我的退货到哪里了？"
- "手机保修多久？"

## 架构解析

### 整体架构

```
浏览器 (SSE 流式)
    |
CustomerServiceController   Flux<String> SSE 端点
    |
CustomerServiceAgent        唯一的 @AiService 入口
    |   ChatMemoryProvider 自动管理上下文
    |   Flux<String> 真流式输出（LangChain4j 自动处理）
    |
    +-- TransactionTools    操作型：MySQL 结构化查询
    +-- KnowledgeTools      知识型：RAG 政策文档检索
```

### Agent 设计：为什么是 1 个 @AiService + 2 组 Tool？

LangChain4j 的 Agent 模式不是"多写几个 @AiService 接口"，而是：

**一个接口注册多个 @Tool，LLM 自主决定调用哪个工具。**

```java
@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"transactionTools", "knowledgeTools"})
@SystemMessage(fromResource = "system-prompts/customer-service-agent.txt")
public interface CustomerServiceAgent {
    Flux<String> chat(@MemoryId String userId, @UserMessage String message);
}
```

不需要任何 Java 路由代码。当用户说"我刚买的耳机有杂音能退吗"，LLM 自己判断：先调 `searchReturnPolicy`，再问手机号查订单。

### 工具分组：TransactionTools vs KnowledgeTools

| 维度 | TransactionTools | KnowledgeTools |
|------|-----------------|----------------|
| 数据源 | MySQL 结构化表 | 非结构化政策文档 |
| 操作类型 | 精确查询 / 数据写入 | 语义检索 |
| 工具数量 | 4 | 4 |
| Spring Bean | `transactionTools` | `knowledgeTools` |

两组工具的能力边界完全不重叠：TransactionTools 不碰文档，KnowledgeTools 不碰数据库。这就是"多 Agent"的正确打开方式 -- 不是按业务流程拆 Agent，而是按能力类型拆工具。

### RAG 管线

启动时：`policies/*.txt`  DocumentSplitter  EmbeddingModel  EmbeddingStore

运行时：用户提问  ContentRetriever  向量检索  返回匹配段落  LLM 整合回答

### RAG 配置要点

- `maxResults(2)`：每次检索返回最多 2 个匹配段落
- `minScore(0.7)`：相似度低于 0.7 的结果被过滤
- `InMemoryEmbeddingStore`：教学环境零依赖，生产环境建议替换为 Redis / Milvus

### ChatMemory：对话上下文管理

```java
@Bean
ChatMemoryProvider chatMemoryProvider() {
    return memoryId -> MessageWindowChatMemory.builder()
            .id(memoryId)
            .chatMemoryStore(new InMemoryChatMemoryStore())
            .maxMessages(20)
            .build();
}
```

- `InMemoryChatMemoryStore`：教学环境零依赖
- `maxMessages(20)`：保留最近 20 条消息，平衡上下文长度和 token 成本
- `@MemoryId` 注解自动传入 userId

### 流式输出：Flux<String> 真流式

```java
// AiService 返回 Flux<String>，LangChain4j 自动使用 streamingChatModel
Flux<String> chat(@MemoryId String userId, @UserMessage String message);

// Controller 直接返回，无需桥接
@GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@ResponseBody
public Flux<String> chatStream(@RequestParam String userId,
                                @RequestParam String message) {
    return agent.chat(userId, message);
}
```

### Spring Boot 集成

本项目使用 `langchain4j-spring-boot-starter`，自动完成以下集成：

- `@AiService` 注解扫描与代理生成
- ChatModel / StreamingChatModel / EmbeddingModel 的自动配置（基于 `application.properties`）
- Spring Bean 自动注入到 @Tool 类中
- 无需手动创建 `AiServices.builder()` 和注册工具

## 项目结构

```
src/main/java/com/langchain4j/
+-- Application.java                    # Spring Boot 启动类
+-- config/
|   +-- ChatMemoryConfig.java           # ChatMemoryProvider Bean + InMemoryChatMemoryStore
|   +-- RAGConfig.java                  # EmbeddingStore + 启动自动建索引
+-- controller/
|   +-- CustomerServiceController.java  # SSE 流式端点 + 历史管理
+-- aiagent/
|   +-- CustomerServiceAgent.java       # @AiService 接口（唯一入口）
+-- tool/
|   +-- TransactionTools.java           # 订单/退货/物流工具（MySQL）
|   +-- KnowledgeTools.java            # RAG 政策检索工具
+-- entity/
|   +-- OrderEntity.java                # 订单实体
|   +-- ReturnEntity.java               # 退货单实体
|   +-- LogisticsEntity.java            # 物流实体
+-- repository/
    +-- OrderRepository.java            # 订单 JPA Repository
    +-- ReturnRepository.java           # 退货单 JPA Repository
    +-- LogisticsRepository.java        # 物流 JPA Repository
```

共 15 个 Java 类，每个职责单一、边界清晰。

### 资源配置

```
src/main/resources/
+-- application.properties              # Spring Boot 主配置（数据源、模型 API）
+-- schema.sql                          # 数据库建表脚本
+-- system-prompts/
|   +-- customer-service-agent.txt      # Agent 系统提示词（角色、技能、约束）
+-- policies/                           # RAG 政策文档源
|   +-- return-policy.txt               # 退换货政策
|   +-- warranty-policy.txt             # 保修政策
|   +-- shipping-policy.txt             # 运费政策
|   +-- faq.txt                         # 常见问题
+-- templates/
    +-- index.html                      # 聊天界面（SSE 流式前端）
```

## 事务工具详解

| 工具方法 | 功能 | 触发场景举例 |
|---------|------|-------------|
| `queryOrdersByPhone` | 根据手机号查订单 | "查一下我的订单" |
| `createReturnRequest` | 创建退货申请 | "我要退货" |
| `queryReturnProgress` | 查退货进度 | "退货到哪了" |
| `queryLogistics` | 查物流轨迹 | "包裹现在在哪" |

## 知识工具详解

| 工具方法 | 功能 | 触发场景举例 |
|---------|------|-------------|
| `searchReturnPolicy` | 退换货政策 | "耳机能退吗" |
| `searchWarrantyPolicy` | 保修政策 | "保修多久" |
| `searchShippingPolicy` | 运费政策 | "运费谁出" |
| `searchFAQ` | 常见问题 | "怎么退货" |

## 与 Cookbook 其他示例的关系

| 示例 | 本项目如何进阶 |
|------|-------------|
| `06-tools`（单工具） | 多组工具 + LLM 自主路由，展示工具协作 |
| `08-rag`（手动检索） | RAG 融入 Tool Calling，检索结果自动进入对话 |
| `03-memoryEachUser`（基本记忆） | Memory + Tool Calling + RAG + 流式，完整闭环 |

## API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/` | GET | 客服聊天界面 |
| `/chat-stream?userId=&message=` | GET | SSE 流式聊天 |
| `/chat-history?userId=` | GET | 查询聊天历史 |
| `/chat-history?userId=` | DELETE | 清除聊天历史 |

## 进阶方向

- **持久化 EmbeddingStore**：替换为 Redis / Milvus（参考 `langchain4j-redis` / `langchain4j-milvus`）
- **持久化 ChatMemory**：替换 `InMemoryChatMemoryStore` 为数据库方案（参考 `04-inMysqlStore`）
- **多用户会话隔离**：生产环境建议引入 Spring Security + Session 管理
- **监控与可观测性**：接入 Langfuse / LangSmith 追踪 LLM 调用链路
- **A/B 测试 Prompt**：不同 System Prompt 对客服质量的影响
- **评估流水线**：对不同 Prompt/Tool 配置做自动化回归测试
