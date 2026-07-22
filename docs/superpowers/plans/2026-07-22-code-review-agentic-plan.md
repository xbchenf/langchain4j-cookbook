# 智能代码审查与优化系统 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 `langchain4j-spring-boot-14-code-review-agentic` 模块——一个展示 6 种 Agentic 工作流模式组合的代码审查教学项目

**Architecture:** 基于 Spring Boot 3.4.2 + LangChain4j 1.14.0 + DeepSeek-chat，通过 `AgenticServices` builder API 编排 8 个 AI Agent + 1 个 Non-AI Agent，以 JUnit 测试驱动端到端工作流执行

**Tech Stack:** Java 17, Spring Boot 3.4.2, LangChain4j BOM 1.14.0, langchain4j-agentic, DeepSeek-chat (OpenAI 兼容协议), JUnit 5, Lombok

## Global Constraints

- Java 17+
- LangChain4j BOM 1.14.0
- 模型: DeepSeek-chat 通过 `langchain4j-open-ai-spring-boot-starter` OpenAI 兼容协议接入
- 测试驱动: 通过 JUnit `@SpringBootTest` 运行完整工作流
- 所有 Agent 接口放在 `com.langchain4j.agent` 包下
- 所有 Domain 类放在 `com.langchain4j.domain` 包下
- API Key 通过环境变量 `DEEPSEEK_API_KEY` 传入
- 遵循 `langchain4j-spring-boot-11-agentic` 的代码风格

---

### Task 1: 项目脚手架

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/pom.xml`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/Application.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/test/resources/application.properties`

**Interfaces:**
- Produces: `Application` Spring Boot 入口类，`pom.xml` 定义所有依赖

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.2</version>
        <relativePath/>
    </parent>

    <groupId>com.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-14-code-review-agentic</artifactId>
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
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-agentic</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>

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

- [ ] **Step 2: 创建 Application.java**

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

- [ ] **Step 3: 创建测试环境 application.properties**

```properties
spring.application.name=CodeReviewAgentic

langchain4j.open-ai.chat-model.api-key=${DEEPSEEK_API_KEY}
langchain4j.open-ai.chat-model.base-url=https://api.deepseek.com
langchain4j.open-ai.chat-model.model-name=deepseek-chat
langchain4j.open-ai.chat-model.log-requests=true
langchain4j.open-ai.chat-model.log-responses=true
```

- [ ] **Step 4: 验证编译**

```bash
cd langchain4j-spring-boot-14-code-review-agentic
mvn compile
```

- [ ] **Step 5: Commit**

```bash
git add langchain4j-spring-boot-14-code-review-agentic/
git commit -m "feat: scaffold code-review-agentic project with pom.xml and Application"
```

---

### Task 2: Domain 类 — CodeIssue + 审查结果

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/domain/CodeIssue.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/domain/SecurityReview.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/domain/PerformanceReview.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/domain/MaintainabilityReview.java`

**Interfaces:**
- Produces: `CodeIssue` — 问题描述基类；`SecurityReview` / `PerformanceReview` / `MaintainabilityReview` — 三维审查结果

- [ ] **Step 1: 创建 CodeIssue.java**

```java
package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CodeIssue {
    /** 问题类型: SECURITY / PERFORMANCE / MAINTAINABILITY / STYLE */
    String type;
    /** 严重程度: HIGH / MEDIUM / LOW */
    String severity;
    /** 问题所在行号 */
    Integer lineNumber;
    /** 问题所在文件 */
    String filePath;
    /** 问题标题，如 "SQL 注入风险" */
    String title;
    /** 问题描述 */
    String description;
    /** 修复建议 */
    String suggestion;
}
```

- [ ] **Step 2: 创建 SecurityReview.java**

```java
package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SecurityReview {
    /** 安全评分 0.0-1.0 */
    Double score;
    /** 发现的安全问题列表 */
    List<CodeIssue> issues;
    /** 总体评价 */
    String summary;
}
```

- [ ] **Step 3: 创建 PerformanceReview.java**

```java
package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PerformanceReview {
    /** 性能评分 0.0-1.0 */
    Double score;
    /** 发现的性能问题列表 */
    List<CodeIssue> issues;
    /** 总体评价 */
    String summary;
}
```

- [ ] **Step 4: 创建 MaintainabilityReview.java**

```java
package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MaintainabilityReview {
    /** 可维护性评分 0.0-1.0 */
    Double score;
    /** 发现的可维护性问题列表 */
    List<CodeIssue> issues;
    /** 总体评价 */
    String summary;
}
```

- [ ] **Step 5: 验证编译**

```bash
mvn compile
```

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: add domain classes — CodeIssue, SecurityReview, PerformanceReview, MaintainabilityReview"
```

---

### Task 3: Domain 类 — 聚合结果 + 报告

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/domain/StaticAnalysisResult.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/domain/CombinedReviewResult.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/domain/FixResult.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/domain/FinalReport.java`

**Interfaces:**
- Consumes: `CodeIssue`, `SecurityReview`, `PerformanceReview`, `MaintainabilityReview` (from Task 2)
- Produces: `StaticAnalysisResult`, `CombinedReviewResult`, `FixResult`, `FinalReport` — 供后续 Agent 使用

- [ ] **Step 1: 创建 StaticAnalysisResult.java**

```java
package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StaticAnalysisResult {
    /** 圈复杂度 */
    Integer cyclomaticComplexity;
    /** 是否超过圈复杂度阈值 (>10 警告) */
    Boolean complexityWarning;
    /** 方法最大行数 */
    Integer maxMethodLines;
    /** 是否超过方法行数阈值 (>50 警告) */
    Boolean methodLengthWarning;
    /** 命名不规范的方法/字段列表 */
    List<String> namingIssues;
    /** 静态分析发现的所有问题 */
    List<CodeIssue> issues;
}
```

- [ ] **Step 2: 创建 CombinedReviewResult.java**

```java
package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CombinedReviewResult {
    /** 安全性审查结果 */
    SecurityReview securityReview;
    /** 性能审查结果 */
    PerformanceReview performanceReview;
    /** 可维护性审查结果 */
    MaintainabilityReview maintainabilityReview;
    /** 综合质量评分：三维评分的平均值 */
    Double qualityScore;
    /** 综合风险等级: HIGH / MEDIUM / LOW */
    String riskLevel;
    /** 所有三维问题合并列表 */
    List<CodeIssue> allIssues;
}
```

- [ ] **Step 3: 创建 FixResult.java**

```java
package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FixResult {
    /** 修复后的代码 */
    String fixedCode;
    /** 修复了哪些问题 */
    String fixDescription;
    /** 修复是否成功 */
    Boolean success;
}
```

- [ ] **Step 4: 创建 FinalReport.java**

```java
package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FinalReport {
    /** 审查的源文件名 */
    String sourceFileName;
    /** 静态分析结果 */
    StaticAnalysisResult staticAnalysis;
    /** AI 审查结果 */
    CombinedReviewResult aiReview;
    /** 修复结果（如有自动修复） */
    FixResult fixResult;
    /** 最终质量评分 0.0-1.0 */
    Double finalScore;
    /** AI 生成的审查总结 */
    String summary;
}
```

- [ ] **Step 5: 验证编译**

```bash
mvn compile
```

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: add domain classes — StaticAnalysisResult, CombinedReviewResult, FixResult, FinalReport"
```

---

### Task 4: Config + Util

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/config/AgenticConfig.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/util/CodeLoader.java`

**Interfaces:**
- Produces: `AgenticConfig` — ChatModel Bean 配置 + 所有 Agent 实例的工厂方法；`CodeLoader` — 从 classpath 加载示例代码

- [ ] **Step 1: 创建 CodeLoader.java**

```java
package com.langchain4j.util;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CodeLoader {

    /** 从 classpath 下的 sample-code/ 目录加载示例代码 */
    public static String load(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("无法加载示例代码: " + resourcePath, e);
        }
    }
}
```

- [ ] **Step 2: 创建 AgenticConfig.java**

```java
package com.langchain4j.config;

import com.langchain4j.agent.*;
import com.langchain4j.nonai.StaticAnalyzer;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class AgenticConfig {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    // ============ Sequential Agents ============

    @Bean
    CodeParser codeParser() {
        return AgenticServices.agentBuilder(CodeParser.class)
                .chatModel(openAiChatModel)
                .outputKey("parsedCode")
                .build();
    }

    @Bean
    IssueIdentifier issueIdentifier() {
        return AgenticServices.agentBuilder(IssueIdentifier.class)
                .chatModel(openAiChatModel)
                .outputKey("codeIssues")
                .build();
    }

    // ============ Parallel Agents ============

    @Bean
    SecurityReviewer securityReviewer() {
        return AgenticServices.agentBuilder(SecurityReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("securityReview")
                .build();
    }

    @Bean
    PerformanceReviewer performanceReviewer() {
        return AgenticServices.agentBuilder(PerformanceReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("perfReview")
                .build();
    }

    @Bean
    MaintainabilityReviewer maintainabilityReviewer() {
        return AgenticServices.agentBuilder(MaintainabilityReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("maintReview")
                .build();
    }

    // ============ Loop Agents ============

    @Bean
    CodeFixer codeFixer() {
        return AgenticServices.agentBuilder(CodeFixer.class)
                .chatModel(openAiChatModel)
                .outputKey("fixResult")
                .build();
    }

    @Bean
    ReReviewer reReviewer() {
        return AgenticServices.agentBuilder(ReReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("finalReview")
                .build();
    }

    // ============ Non-AI Agent ============

    @Bean
    StaticAnalyzer staticAnalyzer() {
        return new StaticAnalyzer();
    }
}
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile
```

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add AgenticConfig and CodeLoader"
```

---

### Task 5: Sequential Agents — CodeParser + IssueIdentifier

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/agent/CodeParser.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/agent/IssueIdentifier.java`

**Interfaces:**
- Produces: `CodeParser` → `IssueIdentifier` 组成 Sequential 工作流的前两步

- [ ] **Step 1: 创建 CodeParser.java**

```java
package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sequential-1: 解析 Java 源代码结构
 * 提取类名、方法签名、字段列表、依赖注入点等结构化信息
 */
public interface CodeParser {

    @UserMessage("""
            请解析以下 Java 源代码，提取其结构化信息。
            
            返回格式：
            1. 类名和包路径
            2. 方法列表（方法名、参数、返回类型、行号范围）
            3. 字段列表（字段名、类型、注解）
            4. 依赖注入点（@Autowired / 构造函数注入）
            5. 异常处理块（try-catch 位置）
            
            只输出结构化解析结果，不要评价代码质量。
            
            源代码：
            {{sourceCode}}
            """)
    @Agent("解析 Java 源代码的结构化信息（类/方法/字段/依赖）")
    String parse(@V("sourceCode") String sourceCode);
}
```

- [ ] **Step 2: 创建 IssueIdentifier.java**

```java
package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sequential-2: 基于代码解析结果，识别潜在问题
 * 接收 CodeParser 的输出，生成问题清单
 */
public interface IssueIdentifier {

    @UserMessage("""
            基于以下代码解析结果，识别代码中的所有潜在问题。
            
            请关注以下类别的问题：
            1. 安全性：SQL 注入、XSS、敏感信息泄露、缺少输入校验
            2. 性能：N+1 查询、不必要的循环、阻塞调用、资源未释放
            3. 可维护性：命名不规范、缺少注释、方法过长、圈复杂度高、异常被吞掉
            4. Bug 风险：空指针风险、类型转换错误、逻辑错误
            
            对于每个问题，请给出：
            - 问题类型（SECURITY / PERFORMANCE / MAINTAINABILITY / STYLE）
            - 严重程度（HIGH / MEDIUM / LOW）
            - 所在行号
            - 问题标题
            - 详细描述
            - 修复建议
            
            代码解析结果：
            {{parsedCode}}
            
            原始代码：
            {{sourceCode}}
            """)
    @Agent("基于代码结构解析结果，识别安全性、性能、可维护性方面的问题")
    String identify(@V("parsedCode") String parsedCode,
                    @V("sourceCode") String sourceCode);
}
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile
```

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add Sequential agents — CodeParser and IssueIdentifier"
```

---

### Task 6: Parallel Agents — SecurityReviewer + PerformanceReviewer + MaintainabilityReviewer

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/agent/SecurityReviewer.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/agent/PerformanceReviewer.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/agent/MaintainabilityReviewer.java`

**Interfaces:**
- Consumes: `sourceCode` + `codeIssues` (from IssueIdentifier output)
- Produces: 三个独立的审查结果，供 Parallel 工作流并行执行

- [ ] **Step 1: 创建 SecurityReviewer.java**

```java
package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Parallel-1: 安全性审查
 * 专门审查 SQL 注入、XSS、敏感信息泄露等安全问题
 */
public interface SecurityReviewer {

    @UserMessage("""
            请专门从【安全性】维度审查以下 Java 代码。
            
            重点关注：
            1. SQL 注入：字符串拼接构建 SQL、未使用参数化查询
            2. XSS 攻击：未转义的用户输入输出到页面
            3. 敏感信息泄露：异常堆栈返回前端、日志打印敏感数据
            4. 输入校验：对外部输入缺少校验和过滤
            5. 权限控制：缺少访问控制注解或检查
            
            对每个发现的问题给出评分（0-1）、详细描述和修复建议。
            最终给出整体安全评分（0.0-1.0）。
            
            已识别的问题列表（参考）：
            {{codeIssues}}
            
            原始代码：
            {{sourceCode}}
            """)
    @Agent("从安全性维度审查代码：SQL注入、XSS、敏感信息泄露、输入校验、权限控制")
    String review(@V("sourceCode") String sourceCode,
                  @V("codeIssues") String codeIssues);
}
```

- [ ] **Step 2: 创建 PerformanceReviewer.java**

```java
package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Parallel-2: 性能审查
 * 专门审查 N+1 查询、阻塞调用、资源泄露等性能问题
 */
public interface PerformanceReviewer {

    @UserMessage("""
            请专门从【性能】维度审查以下 Java 代码。
            
            重点关注：
            1. N+1 查询：循环内执行数据库查询
            2. 阻塞调用：Thread.sleep、同步等待 I/O
            3. 资源泄露：未关闭的连接/流/文件句柄
            4. 不必要的对象创建：循环内 new 大对象
            5. 缓存缺失：重复计算或查询
            
            对每个发现的问题给出评分（0-1）、详细描述和修复建议。
            最终给出整体性能评分（0.0-1.0）。
            
            已识别的问题列表（参考）：
            {{codeIssues}}
            
            原始代码：
            {{sourceCode}}
            """)
    @Agent("从性能维度审查代码：N+1查询、阻塞调用、资源泄露、对象创建、缓存")
    String review(@V("sourceCode") String sourceCode,
                  @V("codeIssues") String codeIssues);
}
```

- [ ] **Step 3: 创建 MaintainabilityReviewer.java**

```java
package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Parallel-3: 可维护性审查
 * 专门审查命名、注释、代码结构、SOLID 原则等可维护性问题
 */
public interface MaintainabilityReviewer {

    @UserMessage("""
            请专门从【可维护性】维度审查以下 Java 代码。
            
            重点关注：
            1. 命名规范：类名/方法名/变量名是否符合 Java 命名惯例
            2. 注释质量：关键逻辑是否有注释、是否有过时注释
            3. 方法长度和复杂度：方法是否过长（>50行）、圈复杂度是否过高
            4. 单一职责：类和方法是否职责清晰
            5. 异常处理：是否吞掉异常、catch 块是否为空
            6. 代码重复：是否有可以抽取的重复代码
            
            对每个发现的问题给出评分（0-1）、详细描述和修复建议。
            最终给出整体可维护性评分（0.0-1.0）。
            
            已识别的问题列表（参考）：
            {{codeIssues}}
            
            原始代码：
            {{sourceCode}}
            """)
    @Agent("从可维护性维度审查代码：命名规范、注释、方法长度、单一职责、异常处理")
    String review(@V("sourceCode") String sourceCode,
                  @V("codeIssues") String codeIssues);
}
```

- [ ] **Step 4: 验证编译**

```bash
mvn compile
```

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add Parallel agents — SecurityReviewer, PerformanceReviewer, MaintainabilityReviewer"
```

---

### Task 7: Loop Agents — CodeFixer + ReReviewer

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/agent/CodeFixer.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/agent/ReReviewer.java`

**Interfaces:**
- Consumes: `combinedReview` (三维审查聚合结果) + `sourceCode`
- Produces: `CodeFixer` → `ReReviewer` 组成 Loop 工作流的修复→重审闭环

- [ ] **Step 1: 创建 CodeFixer.java**

```java
package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Loop-1: 根据审查意见生成修复后的代码
 */
public interface CodeFixer {

    @UserMessage("""
            请根据以下审查意见，修复原始代码中的问题。
            
            修复要求：
            1. 只修复审查意见中指出 MEDIUM 和 HIGH 严重度的问题
            2. 保持代码原有结构和逻辑不变
            3. 修复后代码必须语法正确
            4. 只输出修复后的完整代码，不要添加任何解释
            
            审查意见：
            {{combinedReview}}
            
            原始代码：
            {{sourceCode}}
            """)
    @Agent("根据审查意见修复代码中的问题，输出修复后的完整代码")
    String fix(@V("combinedReview") String combinedReview,
               @V("sourceCode") String sourceCode);
}
```

- [ ] **Step 2: 创建 ReReviewer.java**

```java
package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Loop-2: 对修复后的代码重新审查，给出质量评分
 */
public interface ReReviewer {

    @UserMessage("""
            请重新审查修复后的代码，重点检查：
            1. 之前发现的问题是否已修复
            2. 修复是否引入了新的问题
            3. 代码整体质量是否有提升
            
            给出最终质量评分（0.0-1.0）和简要评价。
            只输出评分和评价，格式为：
            评分: 0.XX
            评价: ...
            
            原始问题列表：
            {{combinedReview}}
            
            修复后的代码：
            {{fixedCode}}
            """)
    @Agent("重新审查修复后的代码，验证问题是否已解决，给出质量评分")
    String review(@V("combinedReview") String combinedReview,
                  @V("fixedCode") String fixedCode);
}
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile
```

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add Loop agents — CodeFixer and ReReviewer"
```

---

### Task 8: Non-AI Agent — StaticAnalyzer

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/main/java/com/langchain4j/nonai/StaticAnalyzer.java`

**Interfaces:**
- Consumes: `sourceCode` (String)
- Produces: `StaticAnalysisResult` — 确定性规则引擎的静态分析结果

- [ ] **Step 1: 创建 StaticAnalyzer.java**

```java
package com.langchain4j.nonai;

import com.langchain4j.domain.CodeIssue;
import com.langchain4j.domain.StaticAnalysisResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Non-AI Agent —— 纯 Java 规则引擎做静态分析
 * 
 * 负责确定性检查（不需要 LLM）：
 * - 圈复杂度估算
 * - 方法行数检查
 * - 命名规范检查
 * 
 * 作为一等公民参与 Agentic 工作流，和 AI Agent 互操作。
 */
public class StaticAnalyzer {

    @Agent(description = "使用规则引擎对源代码进行静态分析（圈复杂度、行数、命名规范）",
           outputKey = "staticResult")
    public StaticAnalysisResult analyze(@V("sourceCode") String sourceCode) {

        System.out.println("[Non-AI Agent] StaticAnalyzer 开始分析...");

        List<CodeIssue> issues = new ArrayList<>();

        // 1. 估算圈复杂度（基于 if/for/while/case 分支数）
        int complexity = countPattern(sourceCode, "\\b(if|for|while|case|catch)\\b");
        boolean complexityWarning = complexity > 10;

        if (complexityWarning) {
            issues.add(CodeIssue.builder()
                    .type("STYLE")
                    .severity("MEDIUM")
                    .title("圈复杂度过高: " + complexity)
                    .description("方法中检测到 " + complexity + " 个分支点，圈复杂度超过建议值 10")
                    .suggestion("考虑拆分复杂方法、提取子方法、使用策略模式")
                    .build());
        }

        // 2. 检查方法行数
        String[] lines = sourceCode.split("\n");
        int maxMethodLines = findLongestMethod(lines);
        boolean methodLengthWarning = maxMethodLines > 50;

        if (methodLengthWarning) {
            issues.add(CodeIssue.builder()
                    .type("STYLE")
                    .severity("LOW")
                    .title("方法过长: " + maxMethodLines + " 行")
                    .description("检测到方法超过 50 行（最长方法 " + maxMethodLines + " 行）")
                    .suggestion("考虑将长方法拆分为多个职责单一的小方法")
                    .build());
        }

        // 3. 检查命名规范
        List<String> namingIssues = checkNaming(lines);

        for (String nameIssue : namingIssues) {
            issues.add(CodeIssue.builder()
                    .type("STYLE")
                    .severity("LOW")
                    .title("命名不规范: " + nameIssue)
                    .description("变量/方法名不符合 Java 命名规范")
                    .suggestion("使用 camelCase 命名变量和方法，PascalCase 命名类")
                    .build());
        }

        System.out.println("[Non-AI Agent] StaticAnalyzer 完成：圈复杂度=" + complexity +
                "，最长方法=" + maxMethodLines + "行，命名问题=" + namingIssues.size());

        return StaticAnalysisResult.builder()
                .cyclomaticComplexity(complexity)
                .complexityWarning(complexityWarning)
                .maxMethodLines(maxMethodLines)
                .methodLengthWarning(methodLengthWarning)
                .namingIssues(namingIssues)
                .issues(issues)
                .build();
    }

    /** 统计正则匹配次数 */
    private int countPattern(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /** 找出最长的代码块（方法） */
    private int findLongestMethod(String[] lines) {
        int maxLen = 0, currentLen = 0;
        boolean inMethod = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches(".*(public|private|protected)\\s+.*\\{.*")) {
                inMethod = true;
                currentLen = 1;
            } else if (inMethod) {
                currentLen++;
                if (trimmed.equals("}") || trimmed.startsWith("}")) {
                    maxLen = Math.max(maxLen, currentLen);
                    inMethod = false;
                }
            }
        }
        return maxLen;
    }

    /** 检查不符合 Java 命名规范的标识符 */
    private List<String> checkNaming(String[] lines) {
        List<String> issues = new ArrayList<>();
        Pattern snakeCasePattern = Pattern.compile("\\b([a-z]+_[a-z]+)\\b");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("String ") || line.contains("int ") ||
                line.contains("boolean ") || line.contains("double ")) {
                Matcher m = snakeCasePattern.matcher(line);
                while (m.find()) {
                    String name = m.group(1);
                    if (name.contains("_") && !name.startsWith("_")) {
                        issues.add("第" + (i + 1) + "行: " + name + " (应使用 camelCase)");
                    }
                }
            }
        }
        return issues;
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add Non-AI Agent — StaticAnalyzer with cyclomatic complexity and naming checks"
```

---

### Task 9: 示例代码文件

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/test/resources/sample-code/UserService.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/test/resources/sample-code/OrderController.java`
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/test/resources/sample-code/PaymentUtil.java`

- [ ] **Step 1: 创建 UserService.java（植入 SQL 注入 + N+1 查询 + 异常吞掉）**

```java
package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private DataSource dataSource;

    public User getUserByName(String name) {
        try {
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            // SQL 注入风险：直接拼接用户输入
            String sql = "SELECT * FROM users WHERE name = '" + name + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                return user;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Order> getUserOrders(Long userId) {
        // N+1 查询：先查订单列表，再逐条查商品
        List<Order> orders = orderDao.findByUserId(userId);
        for (Order order : orders) {
            // 每条订单发起一次查询 — N+1 问题
            List<Item> items = itemDao.findByOrderId(order.getId());
            order.setItems(items);
        }
        return orders;
    }

    public void updateUserEmail(Long userId, String email) {
        // 缺少输入校验：email 可能为空或格式错误
        User user = userDao.findById(userId);
        user.setEmail(email);
        userDao.save(user);
    }

    // 缺少依赖注入声明
    private OrderDao orderDao;
    private ItemDao itemDao;
    private UserDao userDao;
}
```

- [ ] **Step 2: 创建 OrderController.java（植入异常堆栈泄露 + Thread.sleep + 空指针）**

```java
package com.example.demo.controller;

import com.example.demo.entity.Order;
import com.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@RequestParam Long userId) {
        try {
            List<Order> orders = orderService.findByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            // 敏感信息泄露：异常堆栈直接返回给前端
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<Order> getOrderDetail(@PathVariable Long id) {
        try {
            // 阻塞调用：模拟耗时操作
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String status = null;
        // 空指针风险：status 为 null 时调用 equals
        if (status.equals("PAID")) {
            System.out.println("Order is paid");
        }

        Order order = orderService.findById(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/order")
    public ResponseEntity<String> createOrder(@RequestBody Order order) {
        // 方法过长（模拟 80 行）
        // ...参数校验
        // ...库存检查
        // ...价格计算
        // ...优惠券处理
        // ...积分计算
        // ...创建订单
        // ...发送通知
        // ...记录日志
        // ...更新统计
        orderService.create(order);
        return ResponseEntity.ok("success");
    }
}
```

- [ ] **Step 3: 创建 PaymentUtil.java（植入硬编码密钥 + 空指针 + 事务缺失）**

```java
package com.example.demo.util;

import com.example.demo.entity.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentUtil {

    // 硬编码密钥：敏感信息
    private static final String API_SECRET = "sk-abc123def456ghi789";

    public BigDecimal calculateTotal(BigDecimal amount, BigDecimal taxRate) {
        // 空指针风险：amount 和 taxRate 未判 null
        BigDecimal tax = amount.multiply(taxRate);
        return amount.add(tax);
    }

    public void processRefund(Long orderId, BigDecimal amount) {
        // 缺少 @Transactional 注解，但执行多表写操作
        Payment payment = paymentDao.findByOrderId(orderId);
        payment.setStatus("REFUNDED");
        paymentDao.save(payment);

        // 更新库存 — 如果这一步失败，上一步不会回滚
        inventoryDao.increaseStock(orderId);
    }

    private PaymentDao paymentDao;
    private InventoryDao inventoryDao;
}
```

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add sample code files with intentionally planted issues"
```

---

### Task 10: 集成测试 — CodeReviewWorkflowTest

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/src/test/java/com/langchain4j/CodeReviewWorkflowTest.java`

**Interfaces:**
- Consumes: 所有 Agent Beans（从 AgenticConfig）、示例代码（从 CodeLoader）
- Produces: 端到端工作流测试，验证 6 种模式组合运行

- [ ] **Step 1: 创建 CodeReviewWorkflowTest.java**

```java
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

import java.util.concurrent.Executors;

@SpringBootTest
public class CodeReviewWorkflowTest {

    @Autowired private OpenAiChatModel openAiChatModel;

    @Autowired private CodeParser codeParser;
    @Autowired private IssueIdentifier issueIdentifier;
    @Autowired private SecurityReviewer securityReviewer;
    @Autowired private PerformanceReviewer performanceReviewer;
    @Autowired private MaintainabilityReviewer maintainabilityReviewer;
    @Autowired private CodeFixer codeFixer;
    @Autowired private ReReviewer reReviewer;
    @Autowired private StaticAnalyzer staticAnalyzer;

    @Test
    public void testFullReviewWorkflow() {
        // ============ 加载示例代码 ============
        String userServiceCode = CodeLoader.load("sample-code/UserService.java");
        System.out.println("=== 审查开始：UserService.java ===\n");
        System.out.println("--- 原始代码 ---");
        System.out.println(userServiceCode);

        // ============ 1. Sequential: 代码解析 → 问题识别 ============
        UntypedAgent parseAndIdentify = AgenticServices
                .sequenceBuilder()
                .subAgents(codeParser, issueIdentifier)
                .outputKey("codeIssues")
                .build();

        // ============ 2. Non-AI Agent: 静态分析（独立执行）============
        StaticAnalysisResult staticResult = staticAnalyzer.analyze(userServiceCode);
        System.out.println("\n[Non-AI Agent] 静态分析结果:");
        System.out.println("  圈复杂度: " + staticResult.getCyclomaticComplexity() +
                (staticResult.getComplexityWarning() ? " ⚠️" : " ✅"));
        System.out.println("  最长方法: " + staticResult.getMaxMethodLines() + " 行" +
                (staticResult.getMethodLengthWarning() ? " ⚠️" : " ✅"));

        // ============ 3. Parallel: 三维并行审查 ============
        UntypedAgent parallelReview = AgenticServices
                .parallelBuilder()
                .subAgents(securityReviewer, performanceReviewer, maintainabilityReviewer)
                .executor(Executors.newFixedThreadPool(3))
                .outputKey("combinedReview")
                .output(scope -> {
                    String securityResult = (String) scope.readState("securityReview");
                    String perfResult = (String) scope.readState("perfReview");
                    String maintResult = (String) scope.readState("maintReview");

                    System.out.println("\n[SecurityReviewer] 完成");
                    System.out.println("[PerformanceReviewer] 完成");
                    System.out.println("[MaintainabilityReviewer] 完成");

                    return securityResult + "\n---\n" + perfResult + "\n---\n" + maintResult;
                })
                .build();

        // ============ 4. Conditional: 风险路由判断 ============
        // 从并行审查结果中判断风险等级
        String combinedResult = executeWorkflow(parallelReview, userServiceCode);
        boolean isHighRisk = combinedResult.toLowerCase().contains("sql 注入") ||
                combinedResult.toLowerCase().contains("sql注入") ||
                combinedResult.toLowerCase().contains("敏感信息泄露") ||
                combinedResult.toLowerCase().contains("硬编码密钥");

        if (isHighRisk) {
            System.out.println("\n[Conditional] 检测到高风险问题！");
            System.out.println("[Human-in-the-Loop] 高风险问题需要人工确认...");
            System.out.println("[Human-in-the-Loop] ← 模拟: 用户批准修复");
        }

        // ============ 5. Loop: 修复 → 重审 ============
        UntypedAgent fixLoop = AgenticServices
                .loopBuilder()
                .subAgents(codeFixer, reReviewer)
                .outputKey("finalReview")
                .exitCondition(scope -> {
                    String result = (String) scope.readState("finalReview");
                    if (result != null) {
                        System.out.println("[Loop] 审查结果: " +
                                result.substring(0, Math.min(200, result.length())));
                    }
                    // 最多 3 轮，由 maxIterations 控制
                    return false; // 让 maxIterations 控制退出
                })
                .maxIterations(3)
                .build();

        // ============ 6. 执行完整工作流 ============
        System.out.println("\n=== 开始执行工作流 ===\n");

        // 先执行 Sequential: 解析 → 识别
        String issuesOutput = executeWorkflow(parseAndIdentify, userServiceCode);
        System.out.println("\n[Sequential] 代码解析 → 问题识别 完成");

        // 再执行 Parallel + Loop
        if (isHighRisk) {
            String fixedResult = executeWorkflow(fixLoop, combinedResult + "\n" + userServiceCode);
            System.out.println("\n[Loop] 修复 → 重审 完成");
        }

        // ============ 7. 输出最终报告 ============
        System.out.println("\n========================================");
        System.out.println("           最 终 审 查 报 告");
        System.out.println("========================================");
        System.out.println("文件: UserService.java");
        System.out.println("静态分析:");
        System.out.println("  圈复杂度: " + staticResult.getCyclomaticComplexity() +
                (staticResult.getComplexityWarning() ? " ⚠️ 超阈值" : " ✅"));
        System.out.println("  最长方法: " + staticResult.getMaxMethodLines() + " 行" +
                (staticResult.getMethodLengthWarning() ? " ⚠️ 超阈值" : " ✅"));
        System.out.println("风险等级: " + (isHighRisk ? "HIGH" : "MEDIUM/LOW"));
        System.out.println("人工审批: " + (isHighRisk ? "已确认" : "无需"));
        System.out.println("========================================\n");
    }

    private String executeWorkflow(UntypedAgent workflow, String input) {
        try {
            // 将 sourceCode 放入 workflow 的 scope 中
            Object result = AgenticServices.execute(workflow, input);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            System.err.println("工作流执行异常: " + e.getMessage());
            return "";
        }
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn test-compile
```

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add CodeReviewWorkflowTest — end-to-end 6-pattern orchestration"
```

---

### Task 11: 集成验证与 README

**Files:**
- Create: `langchain4j-spring-boot-14-code-review-agentic/README.md`

- [ ] **Step 1: 验证完整编译**

```bash
cd langchain4j-spring-boot-14-code-review-agentic
mvn compile test-compile
```

- [ ] **Step 2: 创建 README.md**

```markdown
# 智能代码审查与优化系统 — LangChain4j Agentic 工作流编排实战

基于 LangChain4j Agentic 框架的代码审查系统，展示 **6 种工作流模式**在真实场景中的有机组合。

## 工作流模式覆盖

| 模式 | Agent | 作用 |
|------|-------|------|
| Sequential | CodeParser → IssueIdentifier | 代码解析 → 问题识别 |
| Non-AI Agent | StaticAnalyzer | 圈复杂度/行数/命名规范（规则引擎） |
| Parallel | SecurityReviewer + PerformanceReviewer + MaintainabilityReviewer | 三维并行审查 |
| Conditional | RiskRouter | 高风险→人工确认 / 中风险→自动修复 |
| Loop | CodeFixer → ReReviewer | 修复→重审直到质量达标 |
| Human-in-the-Loop | 风险确认 | 高风险问题暂停等待人工审批 |

## 快速启动

```bash
export DEEPSEEK_API_KEY=your_key
cd langchain4j-spring-boot-14-code-review-agentic
mvn test -Dtest=CodeReviewWorkflowTest#testFullReviewWorkflow
```

## 示例代码

`src/test/resources/sample-code/` 包含 3 个故意植入问题的 Java 文件：

| 文件 | 植入问题 |
|------|---------|
| UserService.java | SQL 注入、N+1 查询、缺少输入校验 |
| OrderController.java | 异常堆栈泄露、Thread.sleep 阻塞、空指针 |
| PaymentUtil.java | 硬编码密钥、空指针、缺少事务 |

## 与 Cookbook 其他示例的关系

| 示例 | 本项目如何进阶 |
|------|-------------|
| `11-agentic`（独立模式演示） | 6 种模式在**一个连贯工作流**中组合 |
| `13-customerService`（Agent + Tool） | 展示复杂**工作流编排**而非 Agent 设计 |
```

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add README for code-review-agentic project"
```

---

## Plan Summary

| Task | 内容 | 新增文件数 | 预计时间 |
|------|------|-----------|---------|
| 1 | 项目脚手架 | 3 | 5 min |
| 2 | Domain 类 (CodeIssue + 3 reviews) | 4 | 5 min |
| 3 | Domain 类 (聚合结果 + 报告) | 4 | 5 min |
| 4 | Config + Util | 2 | 5 min |
| 5 | Sequential Agents | 2 | 5 min |
| 6 | Parallel Agents | 3 | 5 min |
| 7 | Loop Agents | 2 | 5 min |
| 8 | Non-AI Agent | 1 | 5 min |
| 9 | 示例代码文件 | 3 | 5 min |
| 10 | 集成测试 | 1 | 5 min |
| 11 | 集成验证 + README | 1 | 5 min |
| **总计** | | **26 文件** | **~55 min** |
