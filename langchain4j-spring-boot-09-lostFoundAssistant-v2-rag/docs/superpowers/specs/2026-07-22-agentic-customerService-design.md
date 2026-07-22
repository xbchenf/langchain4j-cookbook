# 电商售后智能客服系统 — 设计规格书

## 项目元信息

| 属性 | 值 |
|------|-----|
| 项目名 | `langchain4j-spring-boot-13-agentic-customerService` |
| 定位 | LangChain4j 多 Agent + RAG 实战教学项目 |
| 业务场景 | 电商售后智能客服（退换货、物流查询、政策检索） |
| 目标用户 | LangChain4j 学习者，具备 Spring Boot 基础 |
| 日期 | 2026-07-22 |

## 1. 教学目标

本项目展示 LangChain4j 三个核心能力在 **一个真实业务场景** 中的正确用法：

1. **多 Agent / Tool Calling**：一个 `@AiService` 接口注册多组工具，LLM 自主决策调用哪个工具
2. **RAG 检索增强**：真实非结构化政策文档 → Embedding → 向量检索 → 自动注入 LLM 上下文
3. **ChatMemory 对话记忆**：框架原生 `ChatMemoryProvider` + `@MemoryId`，多轮对话自动关联

与 cookbook 系列中更简单示例的关系：
- `06-tools`：单个工具的基础用法 → 本项目：**多组工具的 LLM 自主路由**
- `08-rag`：RAG 手动检索示例 → 本项目：**RAG + Tool Calling 融合**
- `03-chatMemoryEachUser`：单用户记忆 → 本项目：**ChatMemory + 工具调用 + 流式，完整闭环**

## 2. 业务功能范围

三个核心子场景，覆盖电商售后最常见的用户需求：

| 子场景 | 涉及能力 | 用户话术示例 |
|--------|---------|------------|
| 退换货咨询与申请 | Tool Calling（查订单/建退货单）+ RAG（退货政策） | "我刚买的耳机有杂音能退吗" |
| 物流查询 | Tool Calling（查物流） | "我的退货寄到哪里了" |
| 政策问答 | RAG 检索 | "退货的运费谁出" |

## 3. 架构设计

### 3.1 整体架构

```
用户（浏览器 SSE）
    │
    ▼
CustomerServiceController          ← SSE 流式端点
    │ Flux<String> (TokenStream)
    ▼
CustomerServiceAgent               ← 唯一 @AiService 入口
    │ @MemoryId + @UserMessage
    │ ChatMemoryProvider 自动注入历史
    │ TokenStream 真流式逐字输出
    │
    ├─ @Tool ── TransactionTools   ← 操作型工具组
    │   ├ queryOrdersByPhone       → MySQL orders 表
    │   ├ createReturnRequest      → MySQL returns 表
    │   ├ queryReturnProgress      → MySQL returns 表
    │   └ queryLogistics           → MySQL logistics 表
    │
    └─ @Tool ── KnowledgeTools     ← 知识型工具组
        ├ searchReturnPolicy       ┐
        ├ searchWarrantyPolicy     │ RAG → EmbeddingStore
        ├ searchShippingPolicy     │      → 政策文档
        └ searchFAQ                ┘
```

### 3.2 核心设计原则

**Agent = 1 个接口 + 多组 Tool**

```java
@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"transactionTools", "knowledgeTools"})
@SystemMessage(fromResource = "system-prompts/customer-service-agent.txt")
public interface CustomerServiceAgent {
    TokenStream chat(@MemoryId String userId, @UserMessage String message);
}
```

教学要点：
- **不需要 Java 路由代码**：LLM 自主决定调用哪个工具
- **不需要手动管理上下文**：`@MemoryId` + `ChatMemoryProvider` 框架自动处理
- **不需要包装阻塞调用**：`TokenStream` 直接产生真流式输出

### 3.3 工具分组：按能力类型而非业务流程

| 维度 | TransactionTools | KnowledgeTools |
|------|-----------------|----------------|
| 数据源 | MySQL 结构化表 | 非结构化政策文档 |
| 操作类型 | 精确查询 / 数据写入 | 语义检索 |
| 返回内容 | 结构化数据（订单号、状态） | 相关文档片段 |
| 工具数量 | 4 | 4 |
| Spring Bean 名 | `transactionTools` | `knowledgeTools` |

两组工具的能力边界天然不重叠，让学习者清晰理解"为什么要分组"：
- TransactionTools 不碰一个文档文件
- KnowledgeTools 不碰一条数据库记录

### 3.4 ChatMemory：唯一方案

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

- 使用 `InMemoryChatMemoryStore`，教学环境零依赖
- `maxMessages(20)`，平衡上下文长度和 token 成本
- 注释说明生产环境可替换为 `langchain4j-spring-boot-04-inMysqlStore` 演示的 DB 持久化

**放弃的方案及理由**：
- ~~AOP 切面存 DB~~：框架已有标准方案，不需要自己造轮子
- ~~Tool 方式手动获取历史~~：浪费一次 tool call，且增加 token 消耗

### 3.5 RAG：启动时自动建索引

```java
@Bean
ApplicationRunner initRagIndex(EmbeddingModel embeddingModel,
                                EmbeddingStore<TextSegment> embeddingStore) {
    return args -> {
        // 加载 policies/ 目录下所有 .txt 文件
        // DocumentSplitter 切分（按段落，maxSegmentSize=300, maxOverlap=50）
        // embeddingModel.embedAll() 向量化
        // embeddingStore.addAll() 存入向量库
    };
}
```

关键设计决策：
- **启动自动建索引**，无需手动调用 `/embedding-index`，零门槛体验
- **`InMemoryEmbeddingStore`**，无需安装外部中间件
- **不做增量索引**（教学环境文档量小，启动全量重建即可）
- 注释说明生产环境替换方案

嵌入模型配置（走 Spring Boot 自动配置）：
```properties
langchain4j.open-ai.embedding-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.embedding-model.api-key=${DASHSCOPE_API_KEY}
langchain4j.open-ai.embedding-model.model-name=text-embedding-v4
```

### 3.6 流式：TokenStream → SSE

```java
@GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@ResponseBody
public Flux<String> chatStream(@RequestParam String userId,
                                @RequestParam String message) {
    return Flux.create(sink -> {
        TokenStream tokenStream = agent.chat(userId, message);
        tokenStream.onNext(sink::next)
                    .onComplete(c -> sink.complete())
                    .onError(sink::error)
                    .start();
    });
}
```

**不引入 WebFlux/Reactor 依赖**：`reactor-core` 已包含在 `spring-boot-starter-web` 中，`Flux.create()` 足够将 `TokenStream` 的回调桥接到 SSE。无需 `spring-boot-starter-webflux`。

### 3.7 前后端约定

| 端点 | 方法 | 说明 |
|------|------|------|
| `/` | GET | 聊天界面 |
| `/chat-stream?userId=xxx&message=xxx` | GET | SSE 流式聊天 |
| `/chat-history?userId=xxx` | GET | 查询聊天历史 |
| `/chat-history?userId=xxx` | DELETE | 清除聊天历史 |

## 4. 数据模型

### 4.1 MySQL 表结构

```sql
-- 订单表
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(30) NOT NULL UNIQUE COMMENT '订单号 ORD20240101...',
    customer_name VARCHAR(50) NOT NULL COMMENT '客户姓名',
    customer_phone VARCHAR(20) NOT NULL COMMENT '客户电话',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    product_price DECIMAL(10,2) COMMENT '商品价格',
    order_status VARCHAR(20) NOT NULL DEFAULT '已完成' COMMENT '订单状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_customer_phone (customer_phone),
    INDEX idx_order_no (order_no)
) COMMENT '订单表';

-- 退货表
CREATE TABLE returns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    return_no VARCHAR(30) NOT NULL UNIQUE COMMENT '退货单号 RET20240101...',
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    reason VARCHAR(500) COMMENT '退货原因',
    status VARCHAR(20) NOT NULL DEFAULT '已提交' COMMENT '状态：已提交/审核中/已退款/已拒绝',
    logistics_no VARCHAR(50) COMMENT '退货运单号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id)
) COMMENT '退货表';

-- 物流表
CREATE TABLE logistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_no VARCHAR(50) NOT NULL UNIQUE COMMENT '物流单号',
    carrier VARCHAR(30) NOT NULL COMMENT '承运商：顺丰/中通/圆通/韵达',
    status VARCHAR(20) NOT NULL DEFAULT '运输中' COMMENT '状态：运输中/已签收/异常',
    current_location VARCHAR(200) COMMENT '当前位置',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tracking_no (tracking_no)
) COMMENT '物流表';
```

### 4.2 RAG 知识库文档

```
policies/
├── return-policy.txt       ← 退换货政策（条件、流程、期限）
├── warranty-policy.txt      ← 保修条款（期限、范围、例外）
├── shipping-policy.txt      ← 运费政策（谁承担、标准）
└── faq.txt                  ← 常见问题（如何申请、材料要求）
```

## 5. 交互流程示例

展示一次对话中 **RAG 检索 + Tool 链式调用 + 多轮对话记忆** 的完整闭环：

```
用户: 我刚买的蓝牙耳机有杂音，能退吗？
Agent: [LLM 自主决策] → 调 searchReturnPolicy("蓝牙耳机退货条件")
       → RAG 检索返回: "电子产品支持7天内无理由退货，质量问题运费卖家承担"
       
Agent: [LLM 整合 RAG 结果追问]
       您好！电子产品在购买7天内可以无理由退货，质量问题退货运费由我们承担。
       请问您的手机号是多少？我帮您查一下订单。

用户: 13812345678
Agent: [LLM 自主决策] → 调 queryOrdersByPhone("13812345678")
       → 返回: [ORD20240715, FreeBuds Pro, ¥768, 7月15日购买, 已完成]
       
Agent: [LLM 判断在 7 天内，建议退货]
       查到您的订单 ORD20240715，华为 FreeBuds Pro，768元，7月15日购买。
       还在7天退货期内！要帮您申请退货吗？

用户: 好的，申请退货
Agent: [LLM 自主决策] → 调 createReturnRequest("ORD20240715", "质量问题-有杂音")
       → 已创建退货单，顺丰单号 SF1234567890
       → 可链式查询 → 调 queryLogistics("SF1234567890")  ← LLM 决定是否需要

Agent: 退货申请已提交！退货单号 RET20240721，顺丰单号 SF1234567890。
       顺丰将在1-2小时内上门取件，退货运费由我们承担。
       退款在收到退货后1-3个工作日到账。还有其他问题吗？
```

这个流程展示了：
1. **LLM 自主决策**：何时查政策、何时查订单、是否查物流，全部 LLM 自己判断
2. **链式调用**：查订单 → 建退货单，两次 Tool call 由 LLM 串联
3. **RAG 自动融合**：检索到的政策条款被 LLM 自然地融入回复
4. **多轮记忆**：ChatMemory 让 Agent 记住前面说的订单号和上下文

## 6. 项目文件结构

```
langchain4j-spring-boot-13-agentic-customerService/
├── pom.xml
├── README.md
├── src/main/java/com/langchain4j/
│   ├── Application.java                         # @SpringBootApplication
│   ├── config/
│   │   ├── ChatMemoryConfig.java                # ChatMemoryProvider Bean
│   │   └── RAGConfig.java                       # EmbeddingStore + 启动建索引
│   ├── controller/
│   │   └── CustomerServiceController.java       # SSE流式 + 历史/清除端点
│   ├── aiagent/
│   │   └── CustomerServiceAgent.java            # @AiService 接口（唯一入口）
│   ├── tool/
│   │   ├── TransactionTools.java                # 订单/退货/物流工具
│   │   └── KnowledgeTools.java                  # RAG 政策检索工具
│   ├── entity/
│   │   ├── OrderEntity.java                     # JPA 实体
│   │   ├── ReturnEntity.java
│   │   └── LogisticsEntity.java
│   └── repository/
│       ├── OrderRepository.java                 # Spring Data JPA
│       ├── ReturnRepository.java
│       └── LogisticsRepository.java
├── src/main/resources/
│   ├── application.properties                   # LLM + DB 配置
│   ├── schema.sql                               # 建表 + 示例数据
│   ├── system-prompts/
│   │   └── customer-service-agent.txt           # Agent 系统提示词
│   ├── policies/                                # RAG 知识库文档
│   │   ├── return-policy.txt
│   │   ├── warranty-policy.txt
│   │   ├── shipping-policy.txt
│   │   └── faq.txt
│   └── templates/
│       └── index.html                           # 聊天界面
```

**15 个 Java 类 + 5 个资源文件**，对比旧项目的 22 个类，减少了 30%。

## 7. 依赖管理

### 7.1 最终依赖列表

| 依赖 | 用途 | 备注 |
|------|------|------|
| `langchain4j-spring-boot-starter` | AI 框架核心 | 保留 |
| `langchain4j-open-ai-spring-boot-starter` | ChatModel + EmbeddingModel | 保留，重用 OpenAI 兼容协议 |
| `spring-boot-starter-web` | Web 服务 + SSE | 保留 |
| `spring-boot-starter-data-jpa` | 数据库 ORM | 保留 |
| `spring-boot-starter-thymeleaf` | 前端模板 | 保留 |
| `mysql-connector-j` | MySQL JDBC 驱动 | 替换 `mysql-connector-java`（已停更） |
| `lombok` | 减少样板代码 | 保留 |

### 7.2 移除的依赖及理由

| 依赖 | 移除理由 |
|------|---------|
| `langchain4j-community-dashscope-spring-boot-starter` (beta) | 未使用，Embedding 走 OpenAI 兼容协议 |
| `hutool-all` | 仅 3 行文件操作，去掉磁盘绕路后完全不需要 |
| `gson` | Jackson 已满足，且无需手写 TypeAdapter |
| `spring-boot-starter-webflux` | TokenStream 桥接 + reactor-core 足够，无需完整 WebFlux |
| `langchain4j-reactor` | 同上，不使用 Reactor 集成模式 |

依赖从 12 个降到 7 个。

## 8. 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| LLM | DeepSeek（OpenAI 兼容协议） | 中文能力强，性价比高 |
| Embedding 模型 | DashScope `text-embedding-v4` | 中文 Embedding 优秀，走 OpenAI 兼容协议 |
| ChatMemory 存储 | `InMemoryChatMemoryStore` | 教学零依赖，注释说明生产方案 |
| Embedding 存储 | `InMemoryEmbeddingStore` | 零依赖，启动自动重建索引 |
| 流式方案 | `TokenStream` → `Flux.create()` → SSE | 真流式，不引入 WebFlux |
| JSON 序列化 | Jackson（Spring Boot 默认） | 零额外依赖 |
| 数据库 | MySQL + JPA | 结构化数据持久化 |

## 9. 配置设计

```properties
# LLM - Chat Model
langchain4j.open-ai.chat-model.api-key=${DEEPSEEK_API_KEY}
langchain4j.open-ai.chat-model.base-url=https://api.deepseek.com
langchain4j.open-ai.chat-model.model-name=deepseek-chat

# LLM - Streaming Chat Model
langchain4j.open-ai.streaming-chat-model.api-key=${DEEPSEEK_API_KEY}
langchain4j.open-ai.streaming-chat-model.base-url=https://api.deepseek.com
langchain4j.open-ai.streaming-chat-model.model-name=deepseek-chat

# LLM - Embedding Model
langchain4j.open-ai.embedding-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.embedding-model.api-key=${DASHSCOPE_API_KEY}
langchain4j.open-ai.embedding-model.model-name=text-embedding-v4

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/customer_service_db
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
```

**与旧项目的关键区别**：
- Embedding 模型走 Spring Boot 自动配置（properties），不再在 Java 代码中手动 `new`
- ChatModel 和 EmbeddingModel 配置风格统一，都是 properties 驱动
- 配置文件简洁，没有大段注释掉的死代码

## 10. README 结构

```markdown
# 电商售后智能客服 — LangChain4j 多 Agent + RAG 实战

## 项目概述
[一句话说明这个项目是什么，展示什么技术]

## 快速开始
### 1. 环境要求
### 2. 创建数据库
### 3. 配置 API Key
### 4. 运行
### 5. 打开浏览器

## 架构解析
### 整体架构图
### Agent 设计：为什么是 1 个 @AiService + 2 组 Tool？
### 工具分组：TransactionTools vs KnowledgeTools
### RAG 管线：政策文档如何被检索？
### ChatMemory：对话上下文如何保持？
### 流式输出：TokenStream 如何工作？

## 代码导读
[逐个文件讲解关键代码，为什么这么写]

## 运行演示
[交互截图，展示典型对话流程]

## 与 Cookbook 其他示例的关系
[和 06-tools、08-rag、03-memory 的关系说明]

## 进阶方向
[生产环境可以考虑的改进]
```

## 11. 与当前项目（失物招领 v2）的对比

| 维度 | 失物招领 v2 | 智能客服（本设计） |
|------|-----------|-----------------|
| Agent 模型 | 4 个 @AiService + Java switch 路由 | 1 个 @AiService + LLM 自主路由 |
| 工具分组理由 | 按业务流程（登记/查询），边界模糊 | 按能力类型（操作/知识），边界清晰 |
| ChatMemory | 3 种方案并存，两种在运行 | 唯一方案：ChatMemoryProvider |
| RAG 检索对象 | DB 表序列化 JSON | 真实非结构化政策文档 |
| RAG 集成方式 | 手动在 Tool 里调 contentRetriever | contentRetriever 注入 @AiService 注解 |
| 流式 | Flux.just(blockingCall) | TokenStream 真流式 SSE |
| ChatModel/EmbeddingModel 配置 | 不一致（auto-config vs 手动 new） | 全部 properties 驱动 |
| 依赖数量 | 12 个 | 7 个 |
| 类数量 | 22 个 | 15 个 |

这份设计规格定义了项目范围、架构、数据模型、交互流程、技术选型和交付物结构。实现时遵循此规格。
