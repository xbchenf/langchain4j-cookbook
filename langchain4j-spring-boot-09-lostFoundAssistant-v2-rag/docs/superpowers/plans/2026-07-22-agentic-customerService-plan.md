# 电商售后智能客服 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建 `langchain4j-spring-boot-13-agentic-customerService` 项目 — 基于 LangChain4j 的电商售后智能客服，展示多 Agent + RAG + ChatMemory 的正确用法。

**Architecture:** 1 个 `@AiService` 接口注册 2 组 `@Tool`（TransactionTools 操作 MySQL 结构化数据，KnowledgeTools 通过 RAG 检索政策文档），`ChatMemoryProvider` 管理多轮对话上下文，`TokenStream` 实现真流式 SSE 输出。

**Tech Stack:** Spring Boot 3.4.2, LangChain4j 1.14.0, MySQL + JPA, DeepSeek (ChatModel), DashScope text-embedding-v4 (EmbeddingModel), InMemoryEmbeddingStore, Thymeleaf

## Global Constraints

- Java 17, Spring Boot 3.4.2, LangChain4j 1.14.0 (BOM)
- 项目根目录: `d:\github\langchain4j-cookbook\langchain4j-spring-boot-13-agentic-customerService`
- 包路径: `com.langchain4j`
- ChatModel: DeepSeek via OpenAI 兼容协议 (properties 自动配置)
- EmbeddingModel: DashScope via OpenAI 兼容协议 (properties 自动配置)
- EmbeddingStore: InMemoryEmbeddingStore (教学环境，注释说明生产方案)
- ChatMemoryStore: InMemoryChatMemoryStore (教学环境，注释说明生产方案)
- 流式方案: @AiService 返回 Flux<String> → Spring MVC SSE (不引入 WebFlux starter，参考 cookbook 03-streaming)
- 依赖数量: 7 个 (不含 parent 和 BOM)，不引入 hutool/gson/webflux/dashscope starter
- 所有 Entity 统一使用 Lombok @Data
- 所有配置走 application.properties，不在 Java 代码中手动 new 模型实例

---
---

### Task 1: 项目脚手架 — pom.xml, Application.java, application.properties, schema.sql

**Files:**
- Create: `d:\github\langchain4j-cookbook\langchain4j-spring-boot-13-agentic-customerService\pom.xml`
- Create: `d:\github\langchain4j-cookbook\langchain4j-spring-boot-13-agentic-customerService\src\main\java\com\langchain4j\Application.java`
- Create: `d:\github\langchain4j-cookbook\langchain4j-spring-boot-13-agentic-customerService\src\main\resources\application.properties`
- Create: `d:\github\langchain4j-cookbook\langchain4j-spring-boot-13-agentic-customerService\src\main\resources\schema.sql`

**Interfaces:**
- Consumes: nothing (first task)
- Produces: `Application.java` 启动类, `pom.xml` 依赖声明, `application.properties` 全量配置, `schema.sql` 建表+示例数据

- [ ] **Step 1: 创建项目目录结构**

```bash
mkdir -p "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/java/com/langchain4j"
mkdir -p "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/resources/templates"
mkdir -p "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/resources/system-prompts"
mkdir -p "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/resources/policies"
mkdir -p "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/java/com/langchain4j/config"
mkdir -p "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/java/com/langchain4j/controller"
mkdir -p "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/java/com/langchain4j/aiagent"
mkdir -p "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/java/com/langchain4j/tool"
mkdir -p "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/java/com/langchain4j/entity"
mkdir -p "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/java/com/langchain4j/repository"
```

- [ ] **Step 2: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.2</version>
        <relativePath/>
    </parent>

    <groupId>com.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-13-agentic-customerService</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>dev.langchain4j</groupId>
                <artifactId>langchain4j-bom</artifactId>
                <version>1.14.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- LangChain4j 核心 -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-spring-boot-starter</artifactId>
        </dependency>

        <!-- OpenAI 兼容协议 (ChatModel + StreamingChatModel + EmbeddingModel) -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
        </dependency>

        <!-- Spring Boot Web (MVC + SSE) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Thymeleaf 模板引擎 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>

        <!-- MySQL JDBC 驱动 (mysql-connector-j 替代已停更的 mysql-connector-java) -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: 创建 Application.java**

```java
package com.langchain4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

- [ ] **Step 4: 创建 application.properties**

```properties
spring.application.name=CustomerServiceAgent

server.port=8082

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/customer_service_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true

# LangChain4j - Chat Model (DeepSeek, OpenAI 兼容协议)
langchain4j.open-ai.chat-model.api-key=${DEEPSEEK_API_KEY}
langchain4j.open-ai.chat-model.base-url=https://api.deepseek.com
langchain4j.open-ai.chat-model.model-name=deepseek-chat
langchain4j.open-ai.chat-model.log-requests=true
langchain4j.open-ai.chat-model.log-responses=true

# LangChain4j - Streaming Chat Model
langchain4j.open-ai.streaming-chat-model.api-key=${DEEPSEEK_API_KEY}
langchain4j.open-ai.streaming-chat-model.base-url=https://api.deepseek.com
langchain4j.open-ai.streaming-chat-model.model-name=deepseek-chat

# LangChain4j - Embedding Model (DashScope, OpenAI 兼容协议)
langchain4j.open-ai.embedding-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.embedding-model.api-key=${DASHSCOPE_API_KEY}
langchain4j.open-ai.embedding-model.model-name=text-embedding-v4
```

- [ ] **Step 5: 创建 schema.sql（建表 + 示例数据）**

```sql
-- ============================================
-- 电商售后智能客服系统 — 数据库初始化脚本
-- ============================================
DROP DATABASE IF EXISTS customer_service_db;
CREATE DATABASE IF NOT EXISTS customer_service_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE customer_service_db;

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    order_no VARCHAR(30) NOT NULL UNIQUE COMMENT '订单号',
    customer_name VARCHAR(50) NOT NULL COMMENT '客户姓名',
    customer_phone VARCHAR(20) NOT NULL COMMENT '客户电话',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    product_price DECIMAL(10,2) COMMENT '商品价格',
    order_status VARCHAR(20) NOT NULL DEFAULT '已完成' COMMENT '订单状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_customer_phone (customer_phone),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 退货表
CREATE TABLE IF NOT EXISTS returns_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    return_no VARCHAR(30) NOT NULL UNIQUE COMMENT '退货单号',
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    reason VARCHAR(500) COMMENT '退货原因',
    status VARCHAR(20) NOT NULL DEFAULT '已提交' COMMENT '状态',
    logistics_no VARCHAR(50) COMMENT '退货运单号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退货表';

-- 物流表
CREATE TABLE IF NOT EXISTS logistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    tracking_no VARCHAR(50) NOT NULL UNIQUE COMMENT '物流单号',
    carrier VARCHAR(30) NOT NULL COMMENT '承运商',
    status VARCHAR(20) NOT NULL DEFAULT '运输中' COMMENT '物流状态',
    current_location VARCHAR(200) COMMENT '当前位置',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tracking_no (tracking_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流表';

-- ============================================
-- 示例数据
-- ============================================
INSERT INTO orders (order_no, customer_name, customer_phone, product_name, product_price, order_status, create_time) VALUES
('ORD20240715', '张伟', '13812345678', 'Huawei FreeBuds Pro 蓝牙耳机', 768.00, '已完成', '2026-07-15 10:30:00'),
('ORD20240710', '张伟', '13812345678', 'iPhone 15 手机壳', 49.00, '已完成', '2026-07-10 14:20:00'),
('ORD20240718', '李娜', '13987654321', 'Samsung Galaxy Watch 6', 1899.00, '已完成', '2026-07-18 09:15:00'),
('ORD20240720', '王强', '13611112222', '小米电动牙刷 T500', 199.00, '已完成', '2026-07-20 16:45:00');

INSERT INTO returns_table (return_no, order_id, reason, status, logistics_no, create_time) VALUES
('RET20240716', 1, '蓝牙连接不稳定，偶尔有杂音', '审核中', NULL, '2026-07-16 11:00:00');

INSERT INTO logistics (tracking_no, carrier, status, current_location) VALUES
('SF1234567890', '顺丰速运', '运输中', '深圳市宝安区中转站');
```

- [ ] **Step 6: Maven 编译验证**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && mvn compile -q
```

Expected: BUILD SUCCESS (会下载依赖，首次可能较慢)

- [ ] **Step 7: 把 pom.xml 加入 git 管理**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && git init && git add pom.xml src/ && git commit -m "feat: project scaffolding - pom, application, config, schema

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---
---

### Task 2: JPA 实体与 Repository 层

**Files:**
- Create: `src/main/java/com/langchain4j/entity/OrderEntity.java`
- Create: `src/main/java/com/langchain4j/entity/ReturnEntity.java`
- Create: `src/main/java/com/langchain4j/entity/LogisticsEntity.java`
- Create: `src/main/java/com/langchain4j/repository/OrderRepository.java`
- Create: `src/main/java/com/langchain4j/repository/ReturnRepository.java`
- Create: `src/main/java/com/langchain4j/repository/LogisticsRepository.java`

**Interfaces:**
- Consumes: `schema.sql` 表结构 (from Task 1)
- Produces:
  - `OrderEntity`: id (Long), orderNo (String), customerName (String), customerPhone (String), productName (String), productPrice (BigDecimal), orderStatus (String), createTime (LocalDateTime)
  - `ReturnEntity`: id (Long), returnNo (String), orderId (Long), reason (String), status (String), logisticsNo (String), createTime (LocalDateTime)
  - `LogisticsEntity`: id (Long), trackingNo (String), carrier (String), status (String), currentLocation (String), updateTime (LocalDateTime)
  - `OrderRepository`: `findByCustomerPhone(String phone)` → `List<OrderEntity>`
  - `ReturnRepository`: `findByOrderId(Long orderId)` → `List<ReturnEntity>`, `findByReturnNo(String returnNo)` → `ReturnEntity`
  - `LogisticsRepository`: `findByTrackingNo(String trackingNo)` → `LogisticsEntity`

- [ ] **Step 1: 创建 OrderEntity.java**

```java
package com.langchain4j.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订单号 */
    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;

    /** 客户姓名 */
    @Column(name = "customer_name", nullable = false, length = 50)
    private String customerName;

    /** 客户电话 */
    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    /** 商品名称 */
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    /** 商品价格 */
    @Column(name = "product_price", precision = 10, scale = 2)
    private BigDecimal productPrice;

    /** 订单状态 */
    @Column(name = "order_status", nullable = false, length = 20)
    private String orderStatus;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 创建 ReturnEntity.java**

```java
package com.langchain4j.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "returns_table")
public class ReturnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 退货单号 */
    @Column(name = "return_no", nullable = false, unique = true, length = 30)
    private String returnNo;

    /** 关联订单ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 退货原因 */
    @Column(name = "reason", length = 500)
    private String reason;

    /** 状态：已提交/审核中/已退款/已拒绝 */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 退货运单号 */
    @Column(name = "logistics_no", length = 50)
    private String logisticsNo;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: 创建 LogisticsEntity.java**

```java
package com.langchain4j.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "logistics")
public class LogisticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 物流单号 */
    @Column(name = "tracking_no", nullable = false, unique = true, length = 50)
    private String trackingNo;

    /** 承运商 */
    @Column(name = "carrier", nullable = false, length = 30)
    private String carrier;

    /** 物流状态 */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 当前位置 */
    @Column(name = "current_location", length = 200)
    private String currentLocation;

    /** 更新时间 */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
```

- [ ] **Step 4: 创建 OrderRepository.java**

```java
package com.langchain4j.repository;

import com.langchain4j.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /** 根据客户电话查询订单 */
    List<OrderEntity> findByCustomerPhone(String customerPhone);
}
```

- [ ] **Step 5: 创建 ReturnRepository.java**

```java
package com.langchain4j.repository;

import com.langchain4j.entity.ReturnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRepository extends JpaRepository<ReturnEntity, Long> {

    /** 根据订单ID查询退货记录 */
    List<ReturnEntity> findByOrderId(Long orderId);

    /** 根据退货单号查询 */
    ReturnEntity findByReturnNo(String returnNo);
}
```

- [ ] **Step 6: 创建 LogisticsRepository.java**

```java
package com.langchain4j.repository;

import com.langchain4j.entity.LogisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogisticsRepository extends JpaRepository<LogisticsEntity, Long> {

    /** 根据物流单号查询 */
    LogisticsEntity findByTrackingNo(String trackingNo);
}
```

- [ ] **Step 7: 编译验证 + 提交**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && mvn compile -q && git add . && git commit -m "feat: add JPA entities and repositories

Co-Authored-By: Claude <noreply@anthropic.com>"
```

Expected: mvn compile BUILD SUCCESS

---
---

### Task 3: 配置 Bean — ChatMemoryConfig, RAGConfig

**Files:**
- Create: `src/main/java/com/langchain4j/config/ChatMemoryConfig.java`
- Create: `src/main/java/com/langchain4j/config/RAGConfig.java`

**Interfaces:**
- Consumes: `application.properties` (EmbeddingModel auto-configured via `langchain4j.open-ai.embedding-model.*`)
- Produces:
  - `ChatMemoryConfig` Bean: `chatMemoryProvider` (ChatMemoryProvider), `getMessages(Object)`, `clear(Object)`
  - `RAGConfig` Beans: `embeddingStore` (EmbeddingStore<TextSegment>), `contentRetriever` (ContentRetriever), `initRagIndex` (ApplicationRunner)

- [ ] **Step 1: 创建 ChatMemoryConfig.java**

```java
package com.langchain4j.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天记忆配置
 *
 * 为每个用户（userId）创建独立的 ChatMemory 实例，
 * 让 AI Agent 在多轮对话中自动记住上下文。
 *
 * 生产环境建议：
 * - 替换 InMemoryChatMemoryStore 为数据库持久化方案
 * - 参考 langchain4j-spring-boot-04-inMysqlStore 的 ChatMemoryStore 实现
 */
@Configuration
public class ChatMemoryConfig {

    /** 线程安全的 ChatMemory 注册表，用于查询/清除指定用户的记忆 */
    private final ConcurrentHashMap<Object, ChatMemory> memoryRegistry = new ConcurrentHashMap<>();

    /**
     * ChatMemoryProvider Bean
     *
     * LangChain4j 通过 @MemoryId 注解传入 userId，
     * 调用此 Provider 获取对应用户的 ChatMemory。
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> memoryRegistry.computeIfAbsent(memoryId, id ->
                MessageWindowChatMemory.builder()
                        .id(id)
                        .chatMemoryStore(new InMemoryChatMemoryStore())
                        .maxMessages(20)  // 保留最近 20 条消息，平衡上下文和 token 成本
                        .build()
        );
    }

    /** 查询用户聊天消息（用于前端展示历史记录） */
    public List<ChatMessage> getMessages(Object memoryId) {
        ChatMemory memory = memoryRegistry.get(memoryId);
        return memory != null ? memory.messages() : List.of();
    }

    /** 清除用户聊天记忆 */
    public void clear(Object memoryId) {
        ChatMemory memory = memoryRegistry.get(memoryId);
        if (memory != null) {
            memory.clear();
        }
    }
}
```

- [ ] **Step 2: 创建 RAGConfig.java**

```java
package com.langchain4j.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

/**
 * RAG（检索增强生成）配置
 *
 * 应用启动时自动加载 policies/ 目录下的政策文档，
 * 切分、向量化并存入 EmbeddingStore。
 *
 * 生产环境建议：
 * - 替换 InMemoryEmbeddingStore 为 Redis / Milvus / Elasticsearch
 * - 使用 langchain4j-redis 或 langchain4j-milvus 等社区集成
 */
@Configuration
@Slf4j
public class RAGConfig {

    /** 内存向量存储（教学环境，零依赖） */
    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * ContentRetriever Bean
     *
     * KnowledgeTools 通过此 Bean 执行 RAG 检索：
     * 用户问题 → Embedding → 向量相似度搜索 → 返回匹配的文档片段
     */
    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                       EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.7)
                .build();
    }

    /**
     * 应用启动时自动构建 RAG 索引
     *
     * 加载 policies/ 目录下所有 .txt 文件：
     * 文档 → 切分 → 向量化 → 存入 EmbeddingStore
     *
     * 教学环境每次启动全量重建（文档量小，秒级完成）。
     * 生产环境应改为增量索引或定时任务。
     */
    @Bean
    ApplicationRunner initRagIndex(EmbeddingModel embeddingModel,
                                    EmbeddingStore<TextSegment> embeddingStore) {
        return args -> {
            try {
                // 定位 policies 目录
                Path policiesDir = Path.of("src/main/resources/policies");
                if (!policiesDir.toFile().exists()) {
                    log.warn("policies 目录不存在: {}, 跳过 RAG 索引构建", policiesDir.toAbsolutePath());
                    return;
                }

                log.info("开始构建 RAG 索引...");

                // 遍历并加载所有 .txt 文件
                java.io.File[] files = policiesDir.toFile()
                        .listFiles((dir, name) -> name.endsWith(".txt"));
                if (files == null || files.length == 0) {
                    log.warn("policies 目录下无 .txt 文件");
                    return;
                }

                DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);

                for (java.io.File file : files) {
                    // 加载文档
                    Document document = FileSystemDocumentLoader.loadDocument(
                            file.toPath(), new TextDocumentParser());

                    // 切分文档段落
                    List<TextSegment> segments = splitter.split(document);

                    // 向量化并存储
                    Response<List<Embedding>> embeddings = embeddingModel.embedAll(segments);
                    embeddingStore.addAll(embeddings.content(), segments);

                    log.info("  indexed: {} → {} segments", file.getName(), segments.size());
                }

                log.info("RAG 索引构建完成！共加载 {} 个文档", files.length);

            } catch (Exception e) {
                log.error("RAG 索引构建失败", e);
            }
        };
    }
}
```

- [ ] **Step 3: 编译验证 + 提交**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && mvn compile -q && git add . && git commit -m "feat: add ChatMemoryConfig and RAGConfig

Co-Authored-By: Claude <noreply@anthropic.com>"
```

Expected: mvn compile BUILD SUCCESS

---
---

### Task 4: Tool 类 — TransactionTools, KnowledgeTools

**Files:**
- Create: `src/main/java/com/langchain4j/tool/TransactionTools.java`
- Create: `src/main/java/com/langchain4j/tool/KnowledgeTools.java`

**Interfaces:**
- Consumes:
  - `OrderRepository.findByCustomerPhone(String)` → `List<OrderEntity>`
  - `ReturnRepository.findByOrderId(Long)` → `List<ReturnEntity>`, `ReturnRepository.save(ReturnEntity)`
  - `LogisticsRepository.findByTrackingNo(String)` → `LogisticsEntity`
  - `ContentRetriever` (from RAGConfig)
- Produces:
  - `TransactionTools`: Spring Bean `"transactionTools"`, 4 个 `@Tool` 方法
  - `KnowledgeTools`: Spring Bean `"knowledgeTools"`, 4 个 `@Tool` 方法

- [ ] **Step 1: 创建 TransactionTools.java**

```java
package com.langchain4j.tool;

import com.langchain4j.entity.LogisticsEntity;
import com.langchain4j.entity.OrderEntity;
import com.langchain4j.entity.ReturnEntity;
import com.langchain4j.repository.LogisticsRepository;
import com.langchain4j.repository.OrderRepository;
import com.langchain4j.repository.ReturnRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 交易操作工具集
 *
 * 提供订单查询、退货申请、退货进度查询、物流查询等操作型工具。
 * 所有工具直接操作 MySQL 数据库中的结构化数据。
 *
 * 工具方法返回值的字段名会自动暴露给 LLM，
 * LLM 根据字段名理解返回数据的含义。
 */
@Component("transactionTools")
@Slf4j
public class TransactionTools {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReturnRepository returnRepository;

    @Autowired
    private LogisticsRepository logisticsRepository;

    /**
     * 根据客户手机号查询订单列表
     */
    @Tool("根据客户手机号查询其在平台上的所有订单记录，返回订单号、商品名称、价格、购买时间、订单状态")
    public List<OrderEntity> queryOrdersByPhone(
            @P("客户手机号码，11位数字") String phone) {
        log.info("查询订单，手机号: {}", phone);
        List<OrderEntity> orders = orderRepository.findByCustomerPhone(phone);
        log.info("找到 {} 条订单记录", orders.size());
        return orders;
    }

    /**
     * 创建退货申请
     */
    @Tool("为客户创建退货申请。需要提供关联订单ID和退货原因。退货单号自动生成，状态初始为\"已提交\"")
    public ReturnEntity createReturnRequest(
            @P("关联的订单ID") Long orderId,
            @P("退货原因描述") String reason) {
        log.info("创建退货申请，订单ID: {}, 原因: {}", orderId, reason);

        ReturnEntity returnEntity = new ReturnEntity();
        returnEntity.setOrderId(orderId);
        returnEntity.setReason(reason);
        returnEntity.setStatus("已提交");

        // 生成退货单号: RET + 日期 + 序号
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        returnEntity.setReturnNo("RET" + datePart + String.format("%04d", System.currentTimeMillis() % 10000));

        // 模拟分配物流单号
        returnEntity.setLogisticsNo("SF" + System.currentTimeMillis() % 10000000000L);

        returnEntity = returnRepository.save(returnEntity);
        log.info("退货申请已创建: {}", returnEntity.getReturnNo());
        return returnEntity;
    }

    /**
     * 查询退货进度
     */
    @Tool("根据退货单号查询退货申请的当前处理状态")
    public ReturnEntity queryReturnProgress(
            @P("退货单号，格式如 RET202407210001") String returnNo) {
        log.info("查询退货进度，退货单号: {}", returnNo);
        ReturnEntity returnEntity = returnRepository.findByReturnNo(returnNo);
        if (returnEntity == null) {
            log.warn("未找到退货单: {}", returnNo);
        }
        return returnEntity;
    }

    /**
     * 查询物流信息
     */
    @Tool("根据物流单号查询包裹的当前位置和运输状态")
    public LogisticsEntity queryLogistics(
            @P("物流单号，如 SF1234567890") String trackingNo) {
        log.info("查询物流，单号: {}", trackingNo);
        LogisticsEntity logistics = logisticsRepository.findByTrackingNo(trackingNo);
        if (logistics == null) {
            log.warn("未找到物流记录: {}", trackingNo);
        }
        return logistics;
    }
}
```

- [ ] **Step 2: 创建 KnowledgeTools.java**

```java
package com.langchain4j.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识检索工具集
 *
 * 通过 RAG（检索增强生成）从公司政策文档中检索相关信息。
 * 四个工具分别对应不同的政策领域，但底层共用同一个 ContentRetriever。
 *
 * 工具分组的意义：
 * - TransactionTools 操作 MySQL 结构化数据
 * - KnowledgeTools 检索非结构化政策文档
 * 两组工具的能力边界完全不重叠，LLM 根据用户问题自主选择调用哪组工具。
 */
@Component("knowledgeTools")
@Slf4j
public class KnowledgeTools {

    @Autowired
    private ContentRetriever contentRetriever;

    /**
     * 通用检索方法：调用 ContentRetriever 执行向量相似度搜索
     */
    private List<String> retrieve(String query) {
        log.info("RAG 检索: {}", query);
        List<String> results = contentRetriever.retrieve(new Query(query))
                .stream()
                .map(content -> content.textSegment().text())
                .toList();
        log.info("RAG 检索返回 {} 条结果", results.size());
        return results;
    }

    @Tool("查询退换货政策：包括退货条件、期限、流程、退款规则等")
    public List<String> searchReturnPolicy(
            @P("用户关于退换货的问题") String query) {
        return retrieve(query);
    }

    @Tool("查询保修政策：包括保修期限、保修范围、不保修的情况、延保服务等")
    public List<String> searchWarrantyPolicy(
            @P("用户关于保修的问题") String query) {
        return retrieve(query);
    }

    @Tool("查询运费政策：包括退货运费承担规则、运费标准、包邮条件等")
    public List<String> searchShippingPolicy(
            @P("用户关于运费的问题") String query) {
        return retrieve(query);
    }

    @Tool("查询常见问题：包括如何申请退货、需要准备什么材料、退款多久到账等操作性问题")
    public List<String> searchFAQ(
            @P("用户的常见操作性问题") String query) {
        return retrieve(query);
    }
}
```

- [ ] **Step 3: 编译验证 + 提交**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && mvn compile -q && git add . && git commit -m "feat: add TransactionTools and KnowledgeTools

Co-Authored-By: Claude <noreply@anthropic.com>"
```

Expected: mvn compile BUILD SUCCESS

---
---

### Task 5: AiService Agent 接口 + 系统提示词

**Files:**
- Create: `src/main/java/com/langchain4j/aiagent/CustomerServiceAgent.java`
- Create: `src/main/resources/system-prompts/customer-service-agent.txt`

**Interfaces:**
- Consumes: `chatMemoryProvider` (ChatMemoryConfig), `transactionTools` (TransactionTools), `knowledgeTools` (KnowledgeTools), `openAiChatModel` / `openAiStreamingChatModel` (auto-configured)
- Produces: `CustomerServiceAgent` Bean — `Flux<String> chat(@MemoryId String userId, @UserMessage String message)`

- [ ] **Step 1: 创建系统提示词 customer-service-agent.txt**

```text
# 角色
你是一位专业、有同理心的电商售后客服专员，名叫"小慧"。你需要帮助客户解决退换货、物流查询以及售后政策方面的问题。始终保持礼貌、耐心、专业的态度。

# 技能

## 技能 1：退换货咨询与处理
- 当客户提出退换货需求时，首先确认客户身份（通过手机号查询订单）
- 调用 searchReturnPolicy 或 searchShippingPolicy 工具查询相关退换货/运费政策
- 根据政策规定，结合客户的订单状态，给出专业建议
- 如果客户确认要退货，调用 createReturnRequest 工具创建退货申请
- 创建退货申请后，主动告知退货单号和预计的退款时间

## 技能 2：物流查询
- 当客户询问"我的退货寄到哪里了"等物流相关问题时：
  1. 先调用 queryReturnProgress 查询退货单号（如果不知道）
  2. 再调用 queryLogistics 查询物流状态
- 用通俗易懂的语言向客户说明物流状态

## 技能 3：售后政策咨询
- 当客户询问退换货条件、保修范围、运费规则等政策性问题时，调用对应工具查询：
  - 退换货条件、流程、期限 → searchReturnPolicy
  - 保修期限、范围、例外 → searchWarrantyPolicy
  - 运费承担规则、标准 → searchShippingPolicy
  - 操作流程、材料准备 → searchFAQ
- 将检索到的政策内容用自然语言向客户解释，不要直接复制政策条文

## 技能 4：订单查询
- 当客户想了解自己的订单信息时，先询问客户手机号
- 调用 queryOrdersByPhone 查询客户的所有订单
- 清晰列出订单信息（订单号、商品、价格、时间、状态）

# 约束
- 仅处理与电商售后相关的问题，拒绝回答无关话题
- 在处理退换货之前，必须先确认客户身份（手机号 + 订单信息）
- 不要编造政策内容，所有政策回答必须基于工具检索结果
- 如果客户描述的问题超出你的处理范围（如技术故障、账号问题），礼貌引导客户联系人工客服
- 回复应条理清晰、简洁明了，符合正常的客服沟通逻辑
- 创建退货申请前，必须确认客户意愿，不得擅自操作
```

- [ ] **Step 2: 创建 CustomerServiceAgent.java**

```java
package com.langchain4j.aiagent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * 电商售后智能客服 Agent
 *
 * 这是整个系统唯一的 @AiService 入口。
 *
 * 设计要点：
 * 1. 一个接口注册所有 Tool，LLM 自主决策调用哪个工具 —— 不需要 Java 路由代码
 * 2. @MemoryId + ChatMemoryProvider 自动管理多轮对话上下文
 * 3. Flux<String> 返回类型实现真流式逐 token 输出（LangChain4j 自动使用 streamingChatModel）
 * 4. 工具按能力类型分两组：transactionTools（操作型）和 knowledgeTools（知识型）
 *
 * 与失物招领 v2 项目的关键区别：
 * - 无需 IntentRouter Agent + switch-case 路由
 * - 无需手动管理聊天历史（ChatHistoryAop / ChatHistoryTools 等方式全部废弃）
 * - 无需 Flux.just() 伪流式包装（Flux 返回类型由 LangChain4j 直接驱动流式 API）
 *
 * Flux<String> 模式来自 cookbook 03-streaming 示例，
 * LangChain4j 检测到 Flux 返回类型后自动使用 streamingChatModel 逐 token 发射。
 */
@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"transactionTools", "knowledgeTools"})
@SystemMessage(fromResource = "system-prompts/customer-service-agent.txt")
public interface CustomerServiceAgent {

    /**
     * 处理用户消息，返回流式 Flux<String>
     *
     * LangChain4j 自动检测 Flux 返回类型，使用 streamingChatModel
     * 逐 token 发射字符串。无需手动桥接 TokenStream。
     *
     * @param userId  用户ID（@MemoryId 自动注入到 ChatMemoryProvider）
     * @param message 用户输入消息（@UserMessage 自动注入到 LLM 请求）
     * @return Flux<String> 流式输出，每次发射一个 token，前端可逐字显示
     */
    Flux<String> chat(@MemoryId String userId, @UserMessage String message);
}
```

- [ ] **Step 3: 编译验证 + 提交**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && mvn compile -q && git add . && git commit -m "feat: add CustomerServiceAgent and system prompt

Co-Authored-By: Claude <noreply@anthropic.com>"
```

Expected: mvn compile BUILD SUCCESS

---
---

### Task 6: Controller — SSE 流式端点 + 聊天历史

**Files:**
- Create: `src/main/java/com/langchain4j/controller/CustomerServiceController.java`

**Interfaces:**
- Consumes: `CustomerServiceAgent.chat(String userId, String message)` → `Flux<String>`, `ChatMemoryConfig.getMessages(Object)` / `ChatMemoryConfig.clear(Object)`
- Produces:
  - `GET /` → `"index"` 模板
  - `GET /chat-stream?userId=&message=` → `Flux<String>` SSE 流式
  - `GET /chat-history?userId=` → `List<Map<String,String>>`
  - `DELETE /chat-history?userId=` → void

- [ ] **Step 1: 创建 CustomerServiceController.java**

```java
package com.langchain4j.controller;

import com.langchain4j.aiagent.CustomerServiceAgent;
import com.langchain4j.config.ChatMemoryConfig;
import dev.langchain4j.data.message.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客服 Controller
 *
 * 提供 4 个端点：
 * - /                 聊天界面
 * - /chat-stream      SSE 真流式聊天
 * - /chat-history     查询聊天历史
 * - /chat-history     清除聊天历史
 */
@org.springframework.stereotype.Controller
public class CustomerServiceController {

    @Autowired
    private CustomerServiceAgent agent;

    @Autowired
    private ChatMemoryConfig chatMemoryConfig;

    /** 聊天界面 */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * SSE 流式聊天端点
     *
     * Agent 返回 Flux<String>，LangChain4j 自动使用 streamingChatModel 逐 token 发射。
     * Controller 直接将 Flux 返回给 Spring MVC，自动转换为 SSE 格式 (data: ...\n\n)。
     *
     * 关键：不引入 spring-boot-starter-webflux。
     * reactor-core 已在 spring-boot-starter-web 中，Spring MVC 原生支持 Flux 返回值 + SSE。
     *
     * 参考：cookbook 03-streaming 示例
     */
    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<String> chatStream(@RequestParam(defaultValue = "user") String userId,
                                    @RequestParam String message) {
        return agent.chat(userId, message);
    }

    /**
     * 查询用户聊天历史
     *
     * 从 ChatMemoryProvider 管理的 ChatMemory 中读取历史消息。
     * 注：InMemoryChatMemoryStore 在应用重启后数据丢失。
     * 生产环境参考 langchain4j-spring-boot-04-inMysqlStore。
     */
    @GetMapping("/chat-history")
    @ResponseBody
    public List<Map<String, String>> getChatHistory(@RequestParam(defaultValue = "user") String userId) {
        List<ChatMessage> messages = chatMemoryConfig.getMessages(userId);
        return messages.stream()
                .map(msg -> Map.of(
                        "role", msg.type().name(),
                        "content", msg.toString()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 清除用户聊天记忆
     */
    @DeleteMapping("/chat-history")
    @ResponseBody
    public void clearChatHistory(@RequestParam(defaultValue = "user") String userId) {
        chatMemoryConfig.clear(userId);
    }
}
```

- [ ] **Step 2: 编译验证 + 提交**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && mvn compile -q && git add . && git commit -m "feat: add CustomerServiceController with SSE streaming

Co-Authored-By: Claude <noreply@anthropic.com>"
```

Expected: mvn compile BUILD SUCCESS

---
---

### Task 7: RAG 知识库文档

**Files:**
- Create: `src/main/resources/policies/return-policy.txt`
- Create: `src/main/resources/policies/warranty-policy.txt`
- Create: `src/main/resources/policies/shipping-policy.txt`
- Create: `src/main/resources/policies/faq.txt`

**Interfaces:**
- Consumes: nothing (独立文档)
- Produces: 4 个 .txt 文件，供 `RAGConfig.initRagIndex` 加载索引

- [ ] **Step 1: 创建 return-policy.txt**

```text
# 退换货政策

## 退货条件
1. 自购买之日起7天内，商品保持完好，支持无理由退货。
2. 商品必须未经使用，不影响二次销售。
3. 退货时需提供完整的包装、配件、赠品。
4. 如商品存在质量问题（非人为损坏），退货期限延长至15天，且退货运费由卖家承担。
5. 以下商品不支持7天无理由退货：已拆封的个人护理用品、已激活的数码产品（如手机、平板已激活）、定制商品、生鲜食品。

## 退货流程
1. 联系客服申请退货，提供订单号和退货原因。
2. 客服审核通过后生成退货单，安排物流上门取件或客户自行寄回。
3. 卖家收到退货后1-3个工作日检验商品。
4. 检验通过后1-5个工作日退款到原支付账户。

## 换货政策
1. 商品存在质量问题时，支持换货。
2. 换货运费由卖家承担。
3. 换货商品如有差价，多退少补。

## 退款规则
1. 退款金额为实际支付金额（不含优惠券）。
2. 使用优惠券的订单退货，优惠券不予退还。
3. 退款到账时间：支付宝/微信1-3工作日，银行卡3-7工作日。
```

- [ ] **Step 2: 创建 warranty-policy.txt**

```text
# 保修政策

## 保修期限
1. 电子产品（手机、平板、耳机、手表等）：自购买之日起保修1年。
2. 大家电（冰箱、洗衣机、空调等）：自购买之日起保修3年。
3. 小家电（电饭煲、电磁炉、豆浆机等）：自购买之日起保修1年。
4. 服装鞋帽类：自购买之日起30天内质量问题可退换。

## 保修范围
保修期内，因产品本身质量问题导致的故障，免费维修。
保修服务包括：免费检测、免费更换故障零部件、免费人工服务。

## 不在保修范围的情况
1. 人为损坏（摔坏、进水、挤压变形等）。
2. 未经授权的拆机或维修。
3. 使用非原装配件导致的损坏。
4. 自然灾害等不可抗力导致的损坏。
5. 已超过保修期限的产品。

## 延保服务
1. 电子产品可购买延保服务，延长保修期至2-3年。
2. 延保价格根据产品类型和延保时长确定。
3. 延保需在购买后30天内或在原保修期到期前办理。
```

- [ ] **Step 3: 创建 shipping-policy.txt**

```text
# 运费政策

## 购物运费
1. 全场满99元包邮。
2. 不满99元，普通快递运费8元，偏远地区（新疆、西藏、内蒙古）15元。
3. 顺丰速运加收10元差价。

## 退货运费
1. 因商品质量问题退货：退货运费由卖家承担，系统自动为买家购买退货运费险。
2. 7天无理由退货（非质量问题）：退货运费由买家承担。
3. 因卖家发错货导致的退货：退货运费由卖家承担。

## 换货运费
1. 商品质量问题换货：双向运费由卖家承担。
2. 非质量问题换货（如尺码不合适）：买家承担来回运费。

## 运费险
1. 部分商品赠送运费险（下单页面会标注）。
2. 运费险赔付金额根据距离和重量计算，通常为8-25元。
```

- [ ] **Step 4: 创建 faq.txt**

```text
# 常见问题 FAQ

## 如何申请退货？
在订单详情页面点击"申请退货"，填写退货原因并提交。客服会在24小时内审核。或者直接联系在线客服，由客服协助办理。

## 退货需要准备什么材料？
退货时需要保留原包装、配件、赠品和发票。缺少任何一项可能影响退货进度或退款金额。

## 退款多久到账？
审核通过后1-5个工作日退款到原支付账户。支付宝/微信通常1-3个工作日到账，银行卡3-7个工作日到账。

## 退货物流怎么安排？
退货申请通过后，系统会自动安排物流上门取件（质量问题由卖家承担运费）。你也可选择自行寄回，保留寄件凭证。

## 商品收到就有问题怎么办？
收到商品24小时内联系客服，提供开箱视频或照片作为凭证。核实后立即安排换货或退货。

## 优惠券订单退货怎么退款？
退款金额为实际支付金额（商品价格减去优惠券金额）。已使用的优惠券不予退还。

## 可以部分退货吗？
同一个订单中有多件商品时，可以单独退其中某一件，退款仅退该商品的实际支付金额。

## 退货后多久可以重新购买？
退货退款完成后即可重新下单，无冷却期限制。
```

- [ ] **Step 5: 提交**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && git add . && git commit -m "feat: add RAG policy documents

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---
---

### Task 8: 前端聊天界面

**Files:**
- Create: `src/main/resources/templates/index.html`

**Interfaces:**
- Consumes: `GET /chat-stream?userId=&message=` (SSE), `GET /chat-history?userId=`, `DELETE /chat-history?userId=`
- Produces: 聊天界面 HTML 页面

- [ ] **Step 1: 创建 index.html**

基于当前失物招领项目的 index.html 模板（保留 UI 样式），调整以适配新后端。关键改动：
- SSE 处理方式不变（ReadableStream 逐块读取）
- 去掉 embedding-index / embedding-query 相关按钮
- 调整 greeting 消息为客服场景

完整代码如下:

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>电商售后智能客服</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Microsoft YaHei', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex; justify-content: center; align-items: center;
            padding: 20px;
        }
        .chat-container {
            width: 100%; max-width: 900px; height: 85vh;
            background: white; border-radius: 20px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            display: flex; flex-direction: column; overflow: hidden;
        }
        .chat-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white; padding: 20px; text-align: center;
        }
        .chat-header h1 {
            font-size: 1.8em; margin-bottom: 10px;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
        }
        .chat-header p { font-size: 0.9em; opacity: 0.9; }
        .user-id-section {
            background: rgba(255,255,255,0.1); padding: 12px;
            border-radius: 10px; margin-top: 10px;
        }
        .user-id-section label { font-size: 0.85em; font-weight: bold; display: block; margin-bottom: 6px; }
        .user-id-input-group { display: flex; gap: 8px; }
        .user-id-input-group input {
            flex: 1; padding: 8px 12px; border: none; border-radius: 5px; font-size: 14px;
        }
        .user-id-input-group button {
            padding: 8px 16px; border: none; border-radius: 5px;
            background: white; color: #667eea; font-weight: bold; cursor: pointer;
            transition: all 0.3s;
        }
        .user-id-input-group button:hover { transform: translateY(-2px); box-shadow: 0 5px 15px rgba(0,0,0,0.2); }
        .chat-messages { flex: 1; overflow-y: auto; padding: 20px; background: #f5f5f5; }
        .message { margin-bottom: 15px; display: flex; align-items: flex-start; }
        .message.user { justify-content: flex-end; }
        .message.ai { justify-content: flex-start; }
        .message-content {
            max-width: 70%; padding: 12px 16px; border-radius: 15px; word-wrap: break-word; white-space: pre-wrap;
        }
        .message.user .message-content {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white; border-bottom-right-radius: 5px;
        }
        .message.ai .message-content {
            background: white; color: #333; border-bottom-left-radius: 5px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }
        .chat-input { padding: 20px; background: white; border-top: 1px solid #ddd; }
        .input-group { display: flex; gap: 10px; }
        .input-group input {
            flex: 1; padding: 12px 16px; border: 2px solid #ddd;
            border-radius: 25px; font-size: 14px; transition: border-color 0.3s;
        }
        .input-group input:focus { outline: none; border-color: #667eea; }
        .input-group button {
            padding: 12px 30px; border: none; border-radius: 25px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white; font-weight: bold; cursor: pointer; transition: all 0.3s;
        }
        .input-group button:hover { transform: translateY(-2px); box-shadow: 0 5px 15px rgba(102,126,234,0.4); }
        .input-group button:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
        .loading {
            display: inline-block; width: 20px; height: 20px;
            border: 3px solid rgba(255,255,255,.3); border-radius: 50%;
            border-top-color: white; animation: spin 1s ease-in-out infinite;
        }
        @keyframes spin { to { transform: rotate(360deg); } }
        .avatar {
            width: 40px; height: 40px; border-radius: 50%; margin: 0 10px;
            display: flex; align-items: center; justify-content: center; font-size: 1.2em;
        }
        .message.ai .avatar { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
        .message.user .avatar { background: #ddd; color: #666; }
    </style>
</head>
<body>
    <div class="chat-container">
        <div class="chat-header">
            <h1>🛒 电商售后智能客服 — 小慧</h1>
            <p>退换货咨询 · 物流查询 · 售后政策解答</p>
            <div class="user-id-section">
                <label>当前用户ID: <span id="currentUserId">customer_001</span></label>
                <div class="user-id-input-group">
                    <input type="text" id="userIdInput" placeholder="请输入用户ID" value="customer_001">
                    <button onclick="setUserId()">设置用户ID</button>
                    <button onclick="clearHistory()">清除对话</button>
                </div>
            </div>
        </div>

        <div class="chat-messages" id="chatMessages"></div>

        <div class="chat-input">
            <div class="input-group">
                <input type="text" id="messageInput" placeholder="输入您的问题..." onkeypress="handleKeyPress(event)">
                <button onclick="sendMessage()" id="sendBtn">发送</button>
            </div>
        </div>
    </div>

    <script>
        let currentUserId = 'customer_001';
        let isStreaming = false;

        window.onload = function() {
            // 显示欢迎消息
            const chatMessages = document.getElementById('chatMessages');
            if (chatMessages.children.length === 0) {
                addMessage('您好！我是售后客服小慧 🎧\n可以帮您处理退换货、查询物流、解答售后政策问题。\n请问有什么可以帮您的？', 'ai');
            }
        };

        function setUserId() {
            const userId = document.getElementById('userIdInput').value.trim();
            if (userId) {
                currentUserId = userId;
                document.getElementById('currentUserId').textContent = userId;
                document.getElementById('chatMessages').innerHTML = '';
                addMessage('您好！我是售后客服小慧 🎧\n可以帮您处理退换货、查询物流、解答售后政策问题。\n请问有什么可以帮您的？', 'ai');
            }
        }

        async function clearHistory() {
            if (confirm('确定要清除对话记录吗？')) {
                try {
                    await fetch(`/chat-history?userId=${currentUserId}`, { method: 'DELETE' });
                    document.getElementById('chatMessages').innerHTML = '';
                    addMessage('对话已清除。请问有什么可以帮您的？', 'ai');
                } catch (error) {
                    console.error('清除失败:', error);
                }
            }
        }

        async function sendMessage() {
            const messageInput = document.getElementById('messageInput');
            const message = messageInput.value.trim();
            if (!message || isStreaming) return;

            addMessage(message, 'user');
            messageInput.value = '';
            isStreaming = true;
            const sendBtn = document.getElementById('sendBtn');
            sendBtn.disabled = true;
            sendBtn.innerHTML = '<span class="loading"></span>';

            try {
                const aiDiv = addMessage('', 'ai');
                const contentDiv = aiDiv.querySelector('.message-content');

                const response = await fetch(
                    `/chat-stream?userId=${encodeURIComponent(currentUserId)}&message=${encodeURIComponent(message)}`
                );
                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let accumulatedText = '';

                while (true) {
                    const { done, value } = await reader.read();
                    if (done) break;
                    // SSE 格式: "data: <token>\n\n"，去掉前缀
                    const chunk = decoder.decode(value, { stream: true });
                    const lines = chunk.split('\n');
                    for (const line of lines) {
                        if (line.startsWith('data: ')) {
                            accumulatedText += line.substring(6);
                        }
                    }
                    contentDiv.textContent = accumulatedText;
                    document.getElementById('chatMessages').scrollTop =
                        document.getElementById('chatMessages').scrollHeight;
                }

            } catch (error) {
                console.error('发送失败:', error);
                addMessage('抱歉，消息发送失败，请稍后再试。', 'ai');
            } finally {
                isStreaming = false;
                sendBtn.disabled = false;
                sendBtn.textContent = '发送';
            }
        }

        function addMessage(text, type) {
            const chatMessages = document.getElementById('chatMessages');
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message ' + type;

            const avatar = document.createElement('div');
            avatar.className = 'avatar';
            avatar.textContent = type === 'user' ? '👤' : '🤖';

            const contentWrapper = document.createElement('div');
            contentWrapper.style.flex = '1';
            const contentDiv = document.createElement('div');
            contentDiv.className = 'message-content';
            contentDiv.textContent = text;

            contentWrapper.appendChild(contentDiv);

            if (type === 'ai') {
                messageDiv.appendChild(avatar);
                messageDiv.appendChild(contentWrapper);
            } else {
                messageDiv.appendChild(contentWrapper);
                messageDiv.appendChild(avatar);
            }

            chatMessages.appendChild(messageDiv);
            chatMessages.scrollTop = chatMessages.scrollHeight;
            return messageDiv;
        }

        function handleKeyPress(event) {
            if (event.key === 'Enter') sendMessage();
        }
    </script>
</body>
</html>
```

- [ ] **Step 2: 提交**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && git add . && git commit -m "feat: add chat frontend UI

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---
---

### Task 9: README.md 文档

**Files:**
- Create: `src/main/resources/readme.md` (保留在 resources 中以兼容旧结构)
- Create: `README.md` (项目根目录)

**Interfaces:**
- Consumes: 所有已完成的代码和配置
- Produces: 完整的项目文档

- [ ] **Step 1: 创建 README.md**

```markdown
# 电商售后智能客服 — LangChain4j 多 Agent + RAG 实战

基于 LangChain4j 的电商售后智能客服系统，展示多 Agent 协作、RAG 检索增强生成、ChatMemory 对话记忆的正确用法。

## 项目定位

本项目是 LangChain4j Cookbook 系列的第 13 个示例，聚焦三个核心能力的实战教学：

| 能力 | 展示方式 | Cookbook 前置知识 |
|------|---------|------------------|
| **多 Agent / Tool Calling** | 1 个 @AiService + 2 组 @Tool，LLM 自主路由 | 建议先看 `06-tools` |
| **RAG 检索增强生成** | 真实政策文档 → Embedding → 向量检索 | 建议先看 `08-rag` |
| **ChatMemory 对话记忆** | ChatMemoryProvider + @MemoryId，多轮对话自动关联 | 建议先看 `03-memoryEachUser` |

与更简单示例的区别：本项目将三个能力整合到一个真实业务场景中，展示它们如何协同工作。

## 业务场景

三个核心子场景，覆盖电商售后最常见的用户需求：

- **退换货咨询与申请**：查退货政策 → 验证订单 → 创建退货单（Tool Calling + RAG）
- **物流查询**：查退货进度 → 查物流轨迹（Tool Calling 链式调用）
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

应用启动时自动加载 `policies/` 目录下的政策文档并构建 RAG 索引。

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
    ↓
CustomerServiceController    ← Flux< String > SSE 端点
    ↓
CustomerServiceAgent        ← 唯一 @AiService 入口
    │   ChatMemoryProvider 自动管理上下文
    │   TokenStream 真流式输出
    ├── TransactionTools     ← 操作型：MySQL 结构化查询
    └── KnowledgeTools       ← 知识型：RAG 政策文档检索
```

### Agent 设计：为什么是 1 个 @AiService + 2 组 Tool？

LangChain4j 的 Agent 模式不是"多写几个 @AiService 接口"，而是：

> **一个接口注册多个 @Tool，LLM 自主决定调用哪个工具。**

```java
@AiService(tools = {"transactionTools", "knowledgeTools"})
public interface CustomerServiceAgent {
    TokenStream chat(@MemoryId String userId, @UserMessage String message);
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

两组工具的能力边界完全不重叠：TransactionTools 不碰文档，KnowledgeTools 不碰数据库。这就是"多 Agent"的正确打开方式——不是按业务流程拆 AGent，而是按能力类型拆工具。

### RAG 管线

```
启动时：policies/*.txt → DocumentSplitter → EmbeddingModel → EmbeddingStore
运行时：用户提问 → ContentRetriever → 向量检索 → 返回匹配段落 → LLM 整合回答
```

### ChatMemory：对话上下文管理

```java
@Bean
ChatMemoryProvider chatMemoryProvider() {
    return memoryId -> MessageWindowChatMemory.builder()
            .id(memoryId)
            .maxMessages(20)
            .build();
}
```

- `InMemoryChatMemoryStore`：教学环境零依赖
- `maxMessages(20)`：平衡上下文长度和 token 成本
- `@MemoryId` 注解自动传入 userId

### 流式输出：TokenStream → SSE

```java
// AiService 返回 TokenStream
TokenStream chat(@MemoryId String userId, @UserMessage String message);

// Controller 桥接到 Flux< String >
Flux.create(sink -> {
    tokenStream.onNext(sink::next)
               .onComplete(c -> sink.complete())
               .start();
});
```

真正的逐 token 流式输出，不是 `Flux.just(blockingCall)`。

## 项目结构

```
src/main/java/com/langchain4j/
├── Application.java                  # Spring Boot 启动类
├── config/
│   ├── ChatMemoryConfig.java         # ChatMemoryProvider Bean
│   └── RAGConfig.java               # EmbeddingStore + 启动自动建索引
├── controller/
│   └── CustomerServiceController.java # SSE 流式 + 历史管理
├── aiagent/
│   └── CustomerServiceAgent.java     # @AiService 接口（唯一入口）
├── tool/
│   ├── TransactionTools.java         # 订单/退货/物流工具
│   └── KnowledgeTools.java          # RAG 政策检索工具
├── entity/
│   ├── OrderEntity.java
│   ├── ReturnEntity.java
│   └── LogisticsEntity.java
└── repository/
    ├── OrderRepository.java
    ├── ReturnRepository.java
    └── LogisticsRepository.java
```

共 15 个 Java 类，每个职责单一、边界清晰。

## 与 Cookbook 其他示例的关系

| 示例 | 本项目如何进阶 |
|------|-------------|
| `06-tools` (单工具) | 多组工具 + LLM 自主路由，展示工具协作 |
| `08-rag` (手动检索) | RAG 融入 Tool Calling，检索结果自动进入对话 |
| `03-memoryEachUser` (基本记忆) | Memory + Tool Calling + RAG + 流式，完整闭环 |

## 进阶方向

- **持久化 EmbeddingStore**：替换为 Redis / Milvus（参考 langchain4j-redis）
- **持久化 ChatMemory**：替换 InMemoryChatMemoryStore 为数据库方案（参考 `04-inMysqlStore`）
- **多用户会话隔离**：生产环境建议引入 Spring Security + Session 管理
- **监控与可观测性**：接入 Langfuse / LangSmith 追踪 LLM 调用链路
- **A/B 测试 Prompt**：不同 System Prompt 对客服质量的影响
```

- [ ] **Step 2: 提交**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && git add . && git commit -m "docs: add comprehensive README

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---
---

### Task 10: 集成验证 — 启动应用并测试 SSE 流式

**Files:**
- Verify: 所有已创建的文件

**Interfaces:**
- Consumes: 所有 Task 1-9 的产出
- Produces: 确认应用可正常运行

- [ ] **Step 1: 确保 MySQL 数据库存在**

```bash
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS customer_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

- [ ] **Step 2: 导入示例数据**

```bash
mysql -u root -proot customer_service_db < "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService/src/main/resources/schema.sql"
```

- [ ] **Step 3: 启动应用**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && mvn spring-boot:run
```

Expected: 启动日志包含 "RAG 索引构建完成！共加载 4 个文档"，应用启动于 8082 端口。

- [ ] **Step 4: 测试非流式端点**

```bash
curl "http://localhost:8082/chat-history?userId=customer_001"
```

Expected: `[]` (空历史)

- [ ] **Step 5: 测试 SSE 流式端点**

```bash
curl -N "http://localhost:8082/chat-stream?userId=customer_001&message=你好"
```

Expected: 逐行输出 SSE 格式的 AI 回复 token。

- [ ] **Step 6: 打开浏览器验证**

访问 http://localhost:8082，在聊天界面输入"我刚买的耳机有杂音能退吗"，观察：
- AI 逐字流式输出
- AI 调用了 searchReturnPolicy 工具（日志可见）
- 多轮对话中 AI 记住了上下文

- [ ] **Step 7: 提交**

```bash
cd "d:/github/langchain4j-cookbook/langchain4j-spring-boot-13-agentic-customerService" && git add . && git commit -m "chore: integration verification complete

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---
