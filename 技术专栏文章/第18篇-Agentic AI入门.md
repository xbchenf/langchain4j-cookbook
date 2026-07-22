# LangChain4j Java AI 应用开发实战（十八）：Agentic AI 入门 - 理解核心概念与单步 Agent

> **摘要**：本文是 Agentic AI 系列的开篇之作，带你理解什么是 Agentic AI、为什么需要它、以及 ReAct 模式的核心原理。通过简历生成器（CvGenerator）这一最简实战案例，你将掌握如何定义 Agent 接口、创建 Agent 实例、调用 Agent 执行任务。同时学习 AgenticScope 的基础概念和 outputKey 机制，为后续深入学习各类工作流编排模式奠定理论基础。

---

## 前言

在前面的文章中，我们学习了 RAG 检索增强生成的各种技术，从 Easy RAG 到 Advanced RAG，再到企业级知识库问答系统。这些技术已经能够解决很多实际问题，但当你面对**更复杂的业务场景**时，会发现单一的技术栈已经力不从心：

**场景 1：多阶段任务**
```
智能招聘系统需要：
1. 根据候选人信息生成简历
2. 根据职位描述优化简历
3. 审查简历质量，不合格则重新生成
4. HR、技术、经理三方评审
5. 根据评审结果安排面试

用 RAG 或 AI Service 能实现吗？❌ 难以编排多个步骤
```

**场景 2：质量保证**
```
代码生成场景：
AI 生成代码 → 人工审查 → 发现问题 → 重新生成 → 再次审查...

手动循环调用？❌ 难以控制终止条件
```

**场景 3：并行处理**
```
晚间计划助手：
- 美食推荐（2 秒）
- 电影推荐（2 秒）
- 活动推荐（2 秒）

串行执行耗时 6 秒？❌ 应该并行执行只需 2 秒
```

这些问题需要一个全新的范式来解决 —— **Agentic AI**。

### 本系列文章规划

本系列将用 **7 篇篇幅**（第18-24篇），循序渐进地讲解 Agentic AI 的完整技术栈：

| 篇章 | 主题 | 核心内容 |
|------|------|---------|
| **第18篇（本篇）** | Agentic AI 入门 | 核心概念 + 单步 Agent 实战 |
| **第19篇** | 顺序工作流 | 多阶段流水线设计模式 |
| **第20篇** | 循环工作流 | 自动迭代直到质量达标 |
| **第21篇** | 并行工作流 | 多 Agent 协同与结果汇总 |
| **第22篇** | 条件分支 | 智能路由与动态决策 |
| **第23篇** | 主管编排与人机协同 | 复杂系统架构 |
| **第24篇** | A2A 协议 | Agent 之间的通信与协作 |

**本篇定位**：作为入门篇，我们只聚焦于：
- ✅ 理解 Agentic AI 是什么、为什么需要它
- ✅ 掌握 ReAct 模式的原理
- ✅ 学会创建和调用**最基础的单步 Agent**
- ✅ 了解 AgenticScope 的基本概念

**不涉及**（留给后续篇章）：
- ❌ 顺序/循环/并行/条件等工作流的具体实现
- ❌ 复杂的状态管理和错误处理
- ❌ 高级的编排技巧

准备好了吗？让我们从最基础的概念开始，进入 Agentic AI 的世界！

---

## 一、什么是 Agentic AI？

### 1.1 从 AI Service 到 Agentic AI

在学习 Agentic AI 之前，我们先回顾一下之前学过的 **AI Service**：

**AI Service（声明式 AI 服务）**：
```java
@AiService
interface Assistant {
    @SystemMessage("你是客服机器人")
    String chat(@UserMessage String question);
}

// 使用
String answer = assistant.chat("如何退款？");
```

**特点**：
- ✅ 简单易用，一行代码调用
- ✅ 适合单次对话场景
- ❌ 无法串联多个步骤
- ❌ 无法自我修正
- ❌ 无状态管理

**Agentic AI（智能体系统）**：
```
用户输入 → Agent 1 → Agent 2 → Agent 3 → ... → 输出结果
              ↓           ↓           ↓
           工具调用    状态管理    错误处理
```

**特点**：
- ✅ 支持多阶段任务编排
- ✅ 有状态（AgenticScope）
- ✅ 可自我修正（循环工作流）
- ✅ 支持并行执行
- ✅ 适合复杂业务场景

**类比**：
- AI Service = 单个员工，完成单一任务
- Agentic AI = 整个团队，协同完成复杂项目

### 1.2 核心概念对比

| 维度 | AI Service | Agentic AI |
|------|-----------|------------|
| **执行方式** | 单步 | 多步编排 |
| **状态管理** | 无状态 | AgenticScope |
| **自我修正** | ❌ | ✅（循环工作流） |
| **并行执行** | ❌ | ✅（并行工作流） |
| **适用场景** | 简单对话 | 复杂业务 |
| **复杂度** | ⭐ | ⭐⭐⭐⭐⭐ |

### 1.3 典型应用场景

**场景 1：智能招聘系统**
```
简历生成 → 简历优化 → 质量审查 → 三方评审 → 面试安排
   ↓           ↓           ↓           ↓           ↓
 Agent 1    Agent 2    Agent 3    Agent 4-6    Agent 7
```

**场景 2：代码开发助手**
```
需求分析 → 代码生成 → 代码审查 → 单元测试 → 文档生成
   ↓           ↓           ↓           ↓           ↓
 Agent 1    Agent 2    Agent 3    Agent 4    Agent 5
```

**场景 3：数据分析平台**
```
数据加载 → 数据清洗 → 特征工程 → 模型训练 → 结果可视化
   ↓           ↓           ↓           ↓           ↓
 Agent 1    Agent 2    Agent 3    Agent 4    Agent 5
```

---

## 二、ReAct 模式：Agent 的思考与行动

### 2.1 什么是 ReAct？

**ReAct** = **Re**asoning（推理）+ **Act**ing（行动）

这是 Agent 的核心工作模式，让 AI 不仅会"回答问题"，还会"思考如何解决"。

### 2.2 ReAct 工作流程

```
┌─────────────┐
│ Observation │ ← 观察：获取当前状态
└──────┬──────┘
       ↓
┌─────────────┐
│   Thought   │ ← 思考：分析下一步该做什么
└──────┬──────┘
       ↓
┌─────────────┐
│   Action    │ ← 行动：执行工具调用或 Agent 调用
└──────┬──────┘
       ↓
  回到 Observation，直到任务完成
```

### 2.3 实际案例

**用户问**："北京明天的天气怎么样？"

**Agent 的思考过程**：
```
Thought: 我需要查询北京的天气信息
Action: 调用 weather_api("北京", "明天")
Observation: {"temperature": "25°C", "condition": "晴"}
Thought: 我已经获得了天气信息，可以回答用户了
Action: final_answer("北京明天晴天，气温 25°C")
```

**传统 AI vs ReAct AI**：

| 维度 | 传统 AI | ReAct AI |
|------|---------|----------|
| **能力** | 基于训练数据回答 | 可以调用外部工具 |
| **实时性** | ❌ 知识截止于训练时间 | ✅ 可以获取实时信息 |
| **可解释性** | ❌ 黑盒 | ✅ 可以看到思考过程 |
| **适用场景** | 通用问答 | 需要外部数据的任务 |

### 2.4 为什么需要 ReAct？

1. **扩展能力边界**
   - 传统 AI：只能基于训练数据回答
   - ReAct AI：可以调用 API、查询数据库、执行代码

2. **提高准确性**
   - 传统 AI：可能产生幻觉
   - ReAct AI：基于真实数据回答

3. **增强可解释性**
   - 传统 AI：不知道答案怎么来的
   - ReAct AI：可以看到完整的思考链条

---

## 三、环境搭建与依赖配置

### 3.1 Maven 依赖

```xml
<dependencies>
    <!-- LangChain4j Spring Boot Starter -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-spring-boot-starter</artifactId>
    </dependency>

    <!-- OpenAI Starter（兼容阿里百炼） -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
    </dependency>

    <!-- Agentic 框架（核心） -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-agentic</artifactId>
    </dependency>
</dependencies>
```

**关键依赖说明**：
- `langchain4j-agentic`：Agentic 框架核心库，提供 Agent 创建工作流编排等功能

### 3.2 配置文件

```properties
# application.properties

# Spring Boot 配置
spring.application.name=LangChain4j-Agentic
server.port=8080

# Chat Model 配置（使用阿里百炼）
langchain4j.open-ai.chat-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.chat-model.api-key=${DASHSCOPE_API_KEY}
langchain4j.open-ai.chat-model.model-name=qwen-max
```

**配置说明**：
- `base-url`：阿里百炼兼容 OpenAI 格式的 API 地址
- `api-key`：从环境变量读取，避免硬编码
- `model-name`：使用通义千问 qwen-max 模型

### 3.3 获取 API Key

**阿里百炼平台**：
1. 访问 https://dashscope.console.aliyun.com/
2. 注册/登录账号
3. 创建 API Key
4. 设置环境变量：
   ```bash
   export DASHSCOPE_API_KEY=your_api_key
   ```

---

## 四、单步 Agent 实战：简历生成器

### 4.1 场景描述

**需求**：根据候选人的生活故事和职业经历，自动生成一份结构化的简历。

**传统方式**：
- 手动编写 Prompt
- 调用 ChatModel
- 解析返回结果
- 代码耦合，难以复用

**Agentic 方式**：
- 定义 Agent 接口
- 使用 `AgenticServices` 创建 Agent
- 一行代码调用
- 清晰、可维护

### 4.2 定义 Agent 接口

```java
package com.langchain4j.agentic._01_basic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 简历生成器接口
 * 
 * @Agent 注解：声明这是一个 Agent，描述其职责
 */
public interface CvGenerator {
    
    /**
     * 根据用户的生活故事和职业经历生成完整的简历
     * 
     * @param userInfo 用户的生活故事和职业经历信息
     * @return 生成的完整简历文本
     */
    @UserMessage("""
        以下是我的人生和职业发展轨迹信息，
        请将其整理成一份清晰完整的简历。
        不要虚构事实，也不要遗漏任何技能或经历。
        这份简历稍后会被进一步清理，目前请确保内容完整。
        只返回简历内容，不要有其他文字。
        
        我的个人经历：{{lifeStory}}
        """)
    @Agent("基于用户提供的信息生成清晰的简历")
    String generateCv(@V("lifeStory") String userInfo);
}
```

**关键点解析**：

1. **`@Agent` 注解**
   ```java
   @Agent("基于用户提供的信息生成清晰的简历")
   ```
   - 标记接口为 Agent
   - 描述 Agent 的职责（用于日志和调试）
   - 类似 `@Tool` 的描述功能

2. **`@UserMessage` 模板**
   ```java
   @UserMessage("""
       ...
       我的个人经历：{{lifeStory}}
       """)
   ```
   - 定义用户消息模板
   - `{{lifeStory}}` 是占位符，会被参数替换
   - 支持多行字符串（Java Text Blocks）

3. **`@V("lifeStory")` 变量绑定**
   ```java
   String generateCv(@V("lifeStory") String userInfo);
   ```
   - 将方法参数 `userInfo` 映射到模板变量 `{{lifeStory}}`
   - 类似 Spring 的 `@RequestParam`

### 4.3 创建并调用 Agent

```java
package com.langchain4j;

import com.langchain4j.agentic._01_basic.CvGenerator;
import com.langchain4j.util.StringLoader;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class _01_CvGeneratorTest {
    
    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Test
    public void testCvGenerator() throws Exception {
        
        // ==================== 第一步：创建 Agent ====================
        
        CvGenerator cvGenerator = AgenticServices
            .agentBuilder(CvGenerator.class)  // 指定 Agent 接口
            .chatModel(openAiChatModel)       // 绑定聊天模型
            .outputKey("masterCv")            // 定义输出键名
            .build();

        // ==================== 第二步：加载用户信息 ====================
        
        String lifeStory = StringLoader.loadFromResource("/documents/user_life_story.txt");
        
        System.out.println("=== 用户信息 ===");
        System.out.println(lifeStory);

        // ==================== 第三步：调用 Agent 生成简历 ====================
        
        String cv = cvGenerator.generateCv(lifeStory);

        // ==================== 第四步：输出结果 ====================
        
        System.out.println("\n=== 生成的简历 ===");
        System.out.println(cv);
    }
}
```

**代码解析**：

1. **创建 Agent**
   ```java
   CvGenerator cvGenerator = AgenticServices
       .agentBuilder(CvGenerator.class)
       .chatModel(openAiChatModel)
       .outputKey("masterCv")
       .build();
   ```
   - `agentBuilder()`：指定 Agent 接口
   - `chatModel()`：绑定聊天模型
   - `outputKey()`：定义输出在 AgenticScope 中的键名
   - `build()`：构建 Agent 实例

2. **调用 Agent**
   ```java
   String cv = cvGenerator.generateCv(lifeStory);
   ```
   - 像调用普通方法一样调用 Agent
   - 底层自动填充 Prompt、调用 LLM、解析结果

### 4.4 运行效果

**输入（user_life_story.txt）**：
```
我叫张三，毕业于北京大学计算机系。
工作经历：
- 2020-2023：阿里巴巴后端开发工程师，负责电商系统开发
- 2023-至今：字节跳动高级后端工程师，负责微服务架构设计

技能：
- Java、Spring Boot、MySQL、Redis
- Docker、Kubernetes、CI/CD
- 熟悉分布式系统设计

项目经验：
- 主导电商平台重构，性能提升 50%
- 设计并实现微服务网关，支撑日均千万级请求
```

**输出（生成的简历）**：
```
# 张三 - 高级后端工程师

## 个人信息
- 姓名：张三
- 学历：北京大学计算机系

## 工作经历

### 字节跳动 | 高级后端工程师（2023-至今）
- 负责微服务架构设计
- 支撑日均千万级请求

### 阿里巴巴 | 后端开发工程师（2020-2023）
- 负责电商系统开发
- 主导电商平台重构，性能提升 50%

## 技能
- 编程语言：Java
- 框架：Spring Boot
- 数据库：MySQL、Redis
- 运维：Docker、Kubernetes、CI/CD
- 架构：分布式系统设计

## 项目经验
- 微服务网关：设计并实现，支撑日均千万级请求
- 电商平台重构：主导，性能提升 50%
```

✅ **成功生成结构化简历！**

### 4.5 工作原理

```
AgenticServices.agentBuilder(CvGenerator.class)
         ↓
   动态代理生成
         ↓
   绑定 ChatModel
         ↓
用户调用 generateCv(lifeStory)
         ↓
   填充 Prompt 模板
         ↓
   调用 LLM
         ↓
   返回结果
```

**与 AI Service 的对比**：

| 维度 | AI Service | Agent |
|------|-----------|-------|
| **创建方式** | `@AiService` 注解 | `AgenticServices.agentBuilder()` |
| **代理生成** | Spring 自动代理 | 手动创建 |
| **灵活性** | 较低 | 更高 |
| **适用场景** | 简单对话 | 工作流编排的基础单元 |

---

## 五、AgenticScope 基础

### 5.1 什么是 AgenticScope？

**AgenticScope** 是 Agent 的**状态容器**，用于存储中间变量和上下文。

```java
// 创建 Agent 时指定 outputKey
CvGenerator cvGenerator = AgenticServices
    .agentBuilder(CvGenerator.class)
    .chatModel(openAiChatModel)
    .outputKey("masterCv")  // 输出键名
    .build();

// 调用后，结果会存储在 AgenticScope 中
String cv = cvGenerator.generateCv(lifeStory);
// AgenticScope.state = {"masterCv": "...生成的简历..."}
```

### 5.2 为什么需要 AgenticScope？

**场景**：在工作流中传递数据

```
Agent 1（简历生成）→ Agent 2（简历优化）
      ↓                    ↓
  masterCv            tailoredCv
  
AgenticScope 负责在 Agent 之间传递 masterCv
```

**作用**：
1. ✅ **数据传递**：Agent 1 的输出 → Agent 2 的输入
2. ✅ **状态管理**：保留中间结果（便于调试和审计）
3. ✅ **上下文共享**：所有 Agent 共享同一个状态容器

### 5.3 outputKey 的作用

```java
.outputKey("masterCv")  // 定义输出键名
```

**作用**：
- 定义 Agent 输出在 AgenticScope 中的键名
- 后续 Agent 可以通过这个键名读取数据
- 类似 Map 的 key-value 结构

**示例**：
```java
// Agent 1
.outputKey("masterCv")
String cv = agent1.generateCv(lifeStory);
// AgenticScope.state = {"masterCv": cv}

// Agent 2
@V("masterCv")  // 读取 Agent 1 的输出
String tailorCv(@V("masterCv") String masterCv, ...)
```

**注意**：
- `outputKey` 的名称必须与下一个 Agent 的输入变量名一致
- 这是工作流数据传递的关键机制（第19篇会详细讲解）

---

## 六、常见问题

### ❓ 问题 1：Agent 和 AI Service 有什么区别？

**回答**：
- **AI Service**：Spring 自动代理，适合简单对话场景
- **Agent**：手动创建，更灵活，是工作流编排的基础单元

**选择建议**：
- 单次对话 → 用 AI Service
- 需要串联多个步骤 → 用 Agent + 工作流

### ❓ 问题 2：什么时候需要 Agentic AI？

**回答**：
- ✅ 多阶段任务（简历生成 → 优化 → 审查）
- ✅ 需要自我修正（代码审查 → 重新生成）
- ✅ 并行处理（多维度分析）
- ✅ 动态决策（智能路由）

**不需要**：
- ❌ 简单问答（用 AI Service 即可）
- ❌ 单次调用（无需编排）

### ❓ 问题 3：outputKey 有什么用？

**回答**：
- 定义 Agent 输出在 AgenticScope 中的键名
- 用于工作流中的数据传递
- 单步 Agent 中可以忽略，工作流中必须配置

---

## 结语

通过本文，我们进入了 Agentic AI 的世界，学习了：

1. ✅ **Agentic AI 核心概念**：与传统 AI 的区别，为什么需要它
2. ✅ **ReAct 模式原理**：推理 + 行动的工作流程
3. ✅ **单步 Agent 实战**：简历生成器（CvGenerator）完整实现
4. ✅ **AgenticScope 基础**：状态容器与 outputKey 机制
5. ✅ **环境搭建**：Maven 依赖配置与 Spring Boot 集成

**关键收获**：
- Agentic AI 是多 Agent 协同的系统，不是单个模型
- ReAct 模式让 Agent 具备推理和行动能力
- `AgenticServices.agentBuilder()` 是创建 Agent 的核心 API
- `outputKey` 定义了输出在 AgenticScope 中的键名，用于工作流数据传递

**下一步行动建议**：
1. **动手实践**：克隆 `langchain4j-spring-boot-11-agentic` 项目，运行 `_01_CvGeneratorTest`
2. **扩展实验**：尝试修改 CvGenerator 的 Prompt，观察输出变化
3. **进阶学习**：阅读下一篇文章《顺序工作流：多阶段流水线设计模式》

在下一篇文章中，我们将学习如何将**多个 Agent 串联成顺序工作流**，实现简历生成 → 简历优化的完整流水线。敬请期待！

---

## 延伸阅读

- **LangChain4j Agentic 官方文档**：https://docs.langchain4j.dev/tutorials/agentic
- **ReAct 论文**：https://arxiv.org/abs/2210.03629
- **Agentic AI 最佳实践**：https://docs.langchain4j.dev/category/agentic-ai

---

**最后更新时间**：2026-06-04  
**作者**：LangChain4j Cookbook 项目组  
**代码仓库**：https://github.com/langchain4j/langchain4j-cookbook
