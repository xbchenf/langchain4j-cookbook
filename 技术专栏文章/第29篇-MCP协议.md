# LangChain4j Java AI 应用开发实战（二十九）：MCP 协议 —— 安全的工具与数据源接入标准

> **摘要**：本文深入讲解 MCP（Model Context Protocol）协议的原理与实战，通过"文件读取 Bot"和"百度地图查询助手"两个完整案例，你将掌握 MCP 的两大传输模式（Stdio 子进程通信 与 HTTP/SSE 远程调用）、学会如何通过 `McpToolProvider` 将远程 MCP 服务器暴露的工具动态注入到 LangChain4j 的 AI Service 中、对比 MCP 与自定义 `@Tool` 注解的核心差异。同时我们还会深入分析 MCP 的安全沙箱设计、资源访问的白名单机制、以及如何利用官方和社区 MCP Server 生态快速扩展 AI 应用的能力边界。

---

## 前言

在前面第 11 篇《Function Calling 工具调用》中，我们学习了如何通过 `@Tool` 注解将 Java 方法暴露给大模型调用。这种方式的优点是简单直观，但也有一个明显的局限：

**场景：被集成方不是 Java**

```
你的团队（Java）：
  构建了一个智能助手，需要集成两个外部工具：
  
  🛠️ 工具 A：内部数据库查询服务
     → 对方团队用 Python 开发，提供了 REST API
  
  🛠️ 工具 B：文件系统浏览服务
     → GitHub 上已有的开源项目，用 TypeScript 实现

传统 @Tool 方案的问题：
  ❌ 必须用 Java 重写工具的调用逻辑（重复造轮子）
  ❌ 工具的实现语言、部署方式各不相同，集成成本高
  ❌ 每个工具的认证、权限、输入校验都需要自己实现
  ❌ 没有统一的标准来管理工具的"发现 → 调用 → 监控"全流程
```

```
你的 AI 助手的真实需求：

传统方案：                          理想方案：
┌──────────────┐                   ┌──────────────┐
│  Java 应用    │                   │  Java 应用    │
│  ┌──────────┐ │                   │  ┌──────────┐ │
│  │ @Tool A  │─┼──→ REST → Python  │  │ MCPClient │─┼──→ MCP Server A
│  ├──────────┤ │                   │  ├──────────┤ │    (Python)
│  │ @Tool B  │─┼──→ REST → TS      │  │ MCPClient │─┼──→ MCP Server B
│  └──────────┘ │                   │  └──────────┘ │    (TypeScript)
│   每个工具需要  │                   │   统一协议，    │
│   单独适配！    │                   │   即插即用！    │
└──────────────┘                   └──────────────┘
```

**MCP（Model Context Protocol）协议正是为解决这个问题而生的！**

MCP 是由 Anthropic 提出的**开放标准协议**，它定义了 LLM 应用与外部工具、数据源之间的标准化通信方式。通过 MCP，你的 Java 应用可以用**同一种方式**集成任何语言实现的工具服务器——就像 USB-C 统一了各种外设的接口一样，MCP 统一了大模型与外部世界的连接方式。

LangChain4j 从 1.14.0 版本开始提供了 `langchain4j-mcp` 模块，让 Java 开发者可以轻松地作为 MCP 客户端消费远程工具，同时也能构建 MCP 服务器对外暴露能力。

---

## 一、MCP 协议核心概念

### 1.1 什么是 MCP 协议？

MCP（Model Context Protocol）是一种**客户端-服务器架构**的开放协议，用于在 LLM 应用与外部工具/数据源之间建立标准化通信。

```
┌──────────────────────────────────────────────────────────────┐
│                      MCP 生态全景                              │
│                                                              │
│  ┌────────────────────┐          ┌────────────────────┐      │
│  │   Java 应用          │          │   Python 应用        │      │
│  │  (LangChain4j)      │          │  (LangChain)        │      │
│  │                    │          │                    │      │
│  │  ┌──────────────┐  │          │  ┌──────────────┐  │      │
│  │  │  MCP Client  │  │          │  │  MCP Client  │  │      │
│  │  └──────┬───────┘  │          │  └──────┬───────┘  │      │
│  └─────────┼──────────┘          └─────────┼──────────┘      │
│            │                                │                 │
│            │        MCP Protocol            │                 │
│            │  ┌──────────────────────┐     │                 │
│            └──┤  • 工具发现 (list)    ├─────┘                 │
│               │  • 工具调用 (call)    │                       │
│               │  • 资源访问 (read)    │                       │
│               │  • 提示模板 (prompt)  │                       │
│               │  • 安全协商 (auth)    │                       │
│               └──────────┬───────────┘                       │
│                          │                                   │
│           ┌──────────────┼──────────────┐                   │
│           ▼              ▼              ▼                   │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │ MCP Server │  │ MCP Server │  │ MCP Server │   ...      │
│  │ (Filesystem)│  │ (Database)  │  │ (BaiduMap) │            │
│  │ Node.js    │  │ Python     │  │ TypeScript │            │
│  └────────────┘  └────────────┘  └────────────┘            │
└──────────────────────────────────────────────────────────────┘
```

**MCP 协议的核心价值**：

| 维度 | 传统方式 | MCP 方式 |
|------|---------|----------|
| **工具集成** | 每个工具单独适配，需用 Java 重写 | 统一协议，任何语言实现的 Server 都能即插即用 |
| **服务发现** | 硬编码工具列表 | 客户端自动从 Server 获取工具清单 |
| **安全控制** | 需要自己实现权限、校验、审计 | 协议内置：白名单目录、环境变量隔离、审计日志 |
| **跨语言** | Java 调用 Python 需要 REST/gRPC 桥梁 | 基于标准 I/O 或 HTTP，天然跨语言 |
| **生态复用** | 工具无法跨项目共享 | 社区 MCP Server 市场，安装即用 |
| **生命周期** | 需自己管理外部进程启停 | MCP Client 自动管理子进程/连接生命周期 |

### 1.2 MCP 协议的核心能力

MCP 协议定义了四大核心原语：

```
┌─────────────────────────────────────────────────┐
│                MCP 四大核心原语                    │
│                                                  │
│  1️⃣ Tools（工具）                                │
│     • 让 LLM 通过 MCP Server 执行操作             │
│     • 类比：Function Calling 的标准化版本          │
│     • 示例：读文件、查数据库、调 API               │
│                                                  │
│  2️⃣ Resources（资源）                            │
│     • 让 LLM 通过 MCP Server 读取数据             │
│     • 类比：RAG 中 DocumentLoader 的标准化版本     │
│     • 示例：文件内容、数据库记录、API 响应          │
│                                                  │
│  3️⃣ Prompts（提示模板）                          │
│     • 让 Server 提供预定义的提示模板               │
│     • 类比：PromptTemplate 的远程版本              │
│     • 示例：代码审查模板、客服话术模板              │
│                                                  │
│  4️⃣ Sampling（采样）                             │
│     • Server 反向请求 Client 进行 LLM 推理        │
│     • 适用于 Agent 嵌套调用的场景                  │
│     • 示例：Server 内部需要 LLM 做中间决策         │
└─────────────────────────────────────────────────┘
```

目前 LangChain4j 的 MCP 模块已经完整支持 **Tools** 原语，这也是本文的重点。

### 1.3 MCP vs 自定义 @Tool：何时该用 MCP？

这是一个非常重要的决策问题。下面是决策指南：

```
是否需要使用 MCP？
        │
        ▼
┌──────────────────┐
│ 工具逻辑是否已经在 │
│ 其他语言中实现？   │
└──────┬───────────┘
       │
   ┌───┴───┐
   │ 是    │ 否
   ▼       ▼
  ✅ MCP  ┌──────────────────┐
          │ 工具是否需要独立  │
          │ 部署/扩容？       │
          └──────┬───────────┘
                 │
             ┌───┴───┐
             │ 是    │ 否
             ▼       ▼
            ✅ MCP  ┌──────────────────┐
                    │ 工具是否要被多个  │
                    │ 应用共享复用？    │
                    └──────┬───────────┘
                           │
                       ┌───┴───┐
                       │ 是    │ 否
                       ▼       ▼
                      ✅ MCP  📌 @Tool 即可
```

**简单总结**：
- 📌 **用 @Tool**：工具逻辑简单、纯 Java 实现、不需要独立部署、仅在一个应用中使用
- ✅ **用 MCP**：工具已用其他语言实现、需要独立部署/扩容、被多个应用共享、来自社区生态

---

## 二、MCP 的两种传输模式

MCP 协议支持两种底层的传输模式，适用于不同的部署场景：

### 2.1 Stdio 传输（标准输入输出）

```
┌─────────────────────────────────────────────┐
│           Stdio 传输模式架构                   │
│                                              │
│  ┌──────────────────┐                       │
│  │   Java 进程        │                       │
│  │  ┌──────────────┐ │    stdin/stdout      │
│  │  │  MCP Client  │◄┼─────────────────────►│
│  │  │  (Stdio      │ │   子进程通信           │
│  │  │   Transport) │ │                      │
│  │  └──────────────┘ │                      │
│  │        │          │                      │
│  │        │ Runtime  │                      │
│  │        │ .exec()  │                      │
│  │        ▼          │                      │
│  │  ┌──────────┐    │                      │
│  │  │ 子进程     │    │                      │
│  │  │ (MCP      │    │                      │
│  │  │  Server)  │    │                      │
│  │  └──────────┘    │                      │
│  └──────────────────┘                      │
│                                              │
│  特点：                                       │
│  ✅ 无需网络端口，进程隔离更安全                  │
│  ✅ 适合本地工具（文件系统、命令行、数据库）       │
│  ✅ 启动/关闭生命周期由父进程管理                │
│  ⚠️ 仅限本地通信，无法远程调用                   │
│  ⚠️ 每个 MCP Server 一个子进程                 │
└─────────────────────────────────────────────┘
```

**Stdio 传输的核心机制**：
- Java 进程通过 `Runtime.exec()` 或 `ProcessBuilder` 启动 MCP Server 作为子进程
- 通过子进程的 **stdin** 发送 JSON-RPC 格式的 MCP 请求
- 通过子进程的 **stdout** 接收 MCP 响应
- stderr 用于 MCP Server 自身的日志输出
- 父进程退出时自动清理子进程

**适用场景**：
- 本地文件系统操作（读写文件、遍历目录）
- 本地命令行工具封装（Git 操作、系统监控）
- 本地数据库访问（SQLite、本地 MySQL）
- 任何不需要远程调用的工具

### 2.2 HTTP/SSE 传输

```
┌─────────────────────────────────────────────┐
│          HTTP/SSE 传输模式架构                  │
│                                              │
│  ┌──────────────────┐                       │
│  │   Java 进程        │    HTTP POST         │
│  │  ┌──────────────┐ │─────────────────────►│
│  │  │  MCP Client  │ │    (JSON-RPC 请求)    │
│  │  │  (Http       │ │                      │
│  │  │   Transport) │◄─────────────────────┤
│  │  └──────────────┘ │    SSE (服务端推送)    │
│  │                   │                       │
│  └──────────────────┘                       │
│                                              │
│                                              │
│           ┌──────────────┐                  │
│           │  MCP Server   │                  │
│           │  (HTTP + SSE) │                  │
│           │  localhost:   │                  │
│           │  3001/sse     │                  │
│           └──────────────┘                  │
│                                              │
│  特点：                                       │
│  ✅ 支持跨机器远程调用                         │
│  ✅ 可以多个客户端共享同一个 Server             │
│  ✅ 适合微服务架构                            │
│  ⚠️ 需要管理网络连接和认证                     │
│  ⚠️ 需要独立部署和运维 Server                 │
└─────────────────────────────────────────────┘
```

**HTTP/SSE 传输的核心机制**：
- 客户端通过 HTTP POST 向 Server 发送 JSON-RPC 请求
- Server 通过 SSE（Server-Sent Events）端点向客户端推送响应和通知
- 支持超时控制、断线重连
- 可以添加 TLS/HTTPS 进行加密通信

**适用场景**：
- 远程 API 调用（天气查询、地图服务、股票数据）
- 微服务架构中的工具服务
- 需要多客户端共享的工具
- 需要独立扩容的工具服务

### 2.3 两种传输模式对比

| 维度 | Stdio 传输 | HTTP/SSE 传输 |
|------|-----------|---------------|
| **通信范围** | 仅限本机进程间 | 支持跨网络 |
| **网络依赖** | 无需网络端口 | 需要 TCP 端口 |
| **安全性** | 最高（进程隔离） | 需要认证 + TLS |
| **启动方式** | Java 父进程自动启动 | 需要预先启动 Server |
| **生命周期** | 同父进程生命周期 | 独立生命周期 |
| **多客户端** | 不支持（1对1） | 支持（1对多） |
| **延迟** | 极低（进程内通信） | 较低（localhost）/中等（远程） |
| **运维复杂度** | 低 | 中等 |
| **扩展性** | 差 | 好 |

**选择建议**：
- 🖥️ **本地工具**（文件、命令行、本地数据库）→ **Stdio**
- 🌐 **远程服务**（API、微服务、SaaS）→ **HTTP/SSE**
- 🔒 **高安全场景**（敏感数据、内网环境）→ **Stdio**
- 📈 **高并发场景**（多客户端共享）→ **HTTP/SSE**

---

## 三、环境准备与依赖配置

### 3.1 核心依赖

在开始实战之前，先了解 MCP 相关的 Maven 依赖：

**纯 Java 项目**（`langchain4j-04-mcp-examples`）：

```xml
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
    <!-- MCP 模块：提供 McpToolProvider、McpClient、传输层 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-mcp</artifactId>
    </dependency>

    <!-- 模型依赖（以 OpenAI 为例） -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai</artifactId>
    </dependency>
</dependencies>
```

**Spring Boot 项目**（`langchain4j-spring-boot-10-mcp`）：

```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-spring-boot-starter</artifactId>
    </dependency>

    <!-- OpenAI Spring Boot Starter（自动配置 ChatModel Bean） -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
    </dependency>

    <!-- MCP 模块 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-mcp</artifactId>
    </dependency>

    <!-- 流式响应支持 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-reactor</artifactId>
    </dependency>
</dependencies>
```

> 📦 **关键依赖说明**：
> - `langchain4j-mcp`：包含 `McpToolProvider`、`DefaultMcpClient`、`StdioMcpTransport`、`HttpMcpTransport` 等核心类
> - MCP 依赖 LangChain4j 1.14.0+ 版本，低版本不支持

### 3.2 Spring Boot 配置

在 `application.properties` 中配置 ChatModel（以 DeepSeek 为例）：

```properties
# 应用基础配置
spring.application.name=LangChain4j-Spring-Boot
server.port=8082

# OpenAI 兼容的 ChatModel 配置（DeepSeek）
langchain4j.open-ai.chat-model.api-key=${DEEPSEEK_API_KEY}
langchain4j.open-ai.chat-model.base-url=https://api.deepseek.com
langchain4j.open-ai.chat-model.model-name=deepseek-chat
langchain4j.open-ai.chat-model.log-requests=true
langchain4j.open-ai.chat-model.log-responses=true

# 流式模型配置（可选）
langchain4j.open-ai.streaming-chat-model.api-key=${DEEPSEEK_API_KEY}
langchain4j.open-ai.streaming-chat-model.base-url=https://api.deepseek.com
langchain4j.open-ai.streaming-chat-model.model-name=deepseek-chat
```

> ⚠️ **注意**：MCP 要求底层 ChatModel 支持 **Function Calling**。大多数主流模型（GPT-4o、GPT-4o-mini、DeepSeek-chat）都支持，但部分轻量模型可能不支持。

---

## 四、实战案例一：基于 Stdio 的文件读取 Bot

第一个案例我们将使用 MCP 官方的 `@modelcontextprotocol/server-filesystem` 服务器，让 AI 助手能够安全地读取本地文件。

### 4.1 前置准备

这个 MCP Server 由 Anthropic 官方维护，提供文件系统操作工具（`read_file`、`list_directory` 等）。

**需要安装**：
- Node.js + npm（MCP Server 运行环境）

**核心原理**：
```
用户提问："读取 file.txt 的内容"
     │
     ▼
┌──────────────┐
│  AI Service  │  检测到需要读取文件
│  (Bot)       │
└──────┬───────┘
       │ 调用工具
       ▼
┌──────────────┐
│ McpTool      │  将调用委托给 MCP Client
│ Provider     │
└──────┬───────┘
       │
       ▼
┌──────────────┐  通过 stdin 发送 JSON-RPC 请求
│ StdioMcp     │  {"method":"tools/call","params":{"name":"read_file",...}}
│ Transport    │
└──────┬───────┘
       │ stdin/stdout
       ▼
┌──────────────┐
│ MCP Server   │  执行文件读取，返回内容
│ (子进程)      │
└──────────────┘
```

### 4.2 定义 AI Service 接口

```java
package dev.langchain4j.example.mcp;

public interface Bot {
    String chat(String prompt);
}
```

这个接口极其简洁——就是一次普通的对话。但当我们通过 `McpToolProvider` 注入 MCP 工具后，`Bot` 将自动获得文件系统操作能力。

### 4.3 完整实现：Step by Step

以下是 `McpToolsExampleOverStdio.java` 的完整实现，我们将它拆解为六个步骤：

```java
package dev.langchain4j.example.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class McpToolsExampleOverStdio {

    public static final String FILE_TO_BE_READ = "src/main/resources/file.txt";

    public static void main(String[] args) throws Exception {

        // ============ Step 1: 配置大语言模型 ============
        ChatModel model = OpenAiChatModel.builder()
                .apiKey("demo")                             // 演示 API Key
                .modelName("gpt-4o-mini")                   // 支持 Function Calling
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        // ============ Step 2: 构建 Stdio 启动命令 ============
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().contains("win");

        List<String> command = new ArrayList<>();
        if (isWindows) {
            command.add("cmd");           // Windows 需要 cmd /c 包装
            command.add("/c");
            command.add("npx");           // npx 执行 npm 包
        } else {
            command.add("npx");
        }

        command.add("-y");                // 自动确认安装
        command.add("@modelcontextprotocol/server-filesystem@0.6.2");
        command.add(new File("src/main/resources")
                .getAbsolutePath());      // 白名单目录（安全关键！）

        // ============ Step 3: 创建 Stdio 传输层 ============
        McpTransport transport = new StdioMcpTransport.Builder()
                .command(command)         // 子进程启动命令
                .logEvents(true)          // 打印通信事件（调试用）
                .build();

        // ============ Step 4: 创建 MCP 客户端 ============
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)     // 绑定传输层
                .build();

        // ============ Step 5: 包装为 ToolProvider ============
        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        // ============ Step 6: 构建 AI Service 并对话 ============
        Bot bot = AiServices.builder(Bot.class)
                .chatModel(model)
                .toolProvider(toolProvider)  // 注入 MCP 工具
                .build();

        try {
            File file = new File(FILE_TO_BE_READ);
            String response = bot.chat(
                "Read the contents of the file "
                + file.getAbsolutePath()
            );
            System.out.println("RESPONSE: " + response);
        } finally {
            mcpClient.close();  // 确保子进程被清理
        }
    }
}
```

### 4.4 关键代码详解

#### 🔑 Step 2：命令构建 —— 跨平台考量

```java
// Linux/macOS:
//   npx -y @modelcontextprotocol/server-filesystem@0.6.2 /path/to/resources

// Windows:
//   cmd /c npx -y @modelcontextprotocol/server-filesystem@0.6.2 C:\path\to\resources

boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
List<String> command = new ArrayList<>();
if (isWindows) {
    command.add("cmd");
    command.add("/c");
    command.add("npx");
} else {
    command.add("npx");
}
command.add("-y");
command.add("@modelcontextprotocol/server-filesystem@0.6.2");
```

**为什么需要这个判断？**

在 Windows 上，`npx` 实际是 `npx.cmd` 批处理脚本。如果不通过 `cmd /c` 包装，Java 的 `Runtime.exec()` 无法直接执行 `.cmd` 文件。这是 Windows 开发中非常容易踩的坑。

**最佳实践**：
1. ✅ 使用 `"npx"` 而非硬编码 `/usr/bin/npm`（跨平台 PATH 查找）
2. ✅ 用 `System.getProperty("os.name")` 动态判断平台
3. ✅ 使用 `List<String>` 而非字符串拼接（参数含空格时更安全）

#### 🔒 Step 2：白名单目录 —— 安全第一

```java
command.add(new File("src/main/resources").getAbsolutePath());
```

**这是 MCP 安全模型的核心！**

`@modelcontextprotocol/server-filesystem` 要求启动时指定一个或多个**允许访问的目录**。MCP Server 只会操作这些白名单目录内的文件，无法访问系统的其他部分：

```
MCP Server 文件访问范围：

允许：                          拒绝：
✅ src/main/resources/file.txt  ❌ /etc/passwd
✅ src/main/resources/docs/     ❌ ~/.ssh/id_rsa
✅ src/main/resources/../       ❌ ../../../
   (白名单内的相对路径)            (跳出白名单的路径)
```

这种设计类似于 Docker 的 Volume 挂载——你显式声明哪些目录对容器可见。即使 AI "要求" MCP Server 读取 `/etc/passwd`，Server 也会拒绝，因为该路径不在白名单中。

#### 🔗 Step 3：StdioMcpTransport 的工作原理

```java
McpTransport transport = new StdioMcpTransport.Builder()
        .command(command)
        .logEvents(true)
        .build();
```

当你调用 `.build()` 时，`StdioMcpTransport` 内部会执行：

```
1. ProcessBuilder pb = new ProcessBuilder(command);
2. Process process = pb.start();
3. 获取 process.getOutputStream() → 用于发送 MCP 请求（写入子进程 stdin）
4. 获取 process.getInputStream()  → 用于接收 MCP 响应（读取子进程 stdout）
5. 获取 process.getErrorStream()  → 用于接收 Server 日志（读取子进程 stderr）
```

**`.logEvents(true)` 的作用**：开启后会打印所有 stdin/stdout 通信内容，对调试非常有帮助：

```
[STDIO] → {"jsonrpc":"2.0","method":"tools/list","id":1}
[STDIO] ← {"jsonrpc":"2.0","result":{"tools":[...]},"id":1}
[STDIO] → {"jsonrpc":"2.0","method":"tools/call","params":{...},"id":2}
[STDIO] ← {"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Kaboom!"}]},"id":2}
```

> ⚠️ 生产环境建议关闭 `logEvents`，避免日志量过大。

#### 📦 Step 5：McpToolProvider —— MCP 与 LangChain4j 的桥梁

```java
ToolProvider toolProvider = McpToolProvider.builder()
        .mcpClients(List.of(mcpClient))
        .build();
```

`McpToolProvider` 是 LangChain4j 与 MCP 之间的关键桥梁：

```
┌───────────────────────────────────────────────────┐
│              McpToolProvider 桥梁作用                │
│                                                    │
│  AiServices                                        │
│     │ 需要工具列表                                   │
│     ▼                                              │
│  ┌──────────────────────┐                         │
│  │  McpToolProvider     │                         │
│  │                      │                         │
│  │  1. 从 MCP Client    │                         │
│  │     获取工具列表      │──── MCP Client ────► MCP Server
│  │                      │     tools/list          │
│  │  2. 将 MCP 工具描述  │                         │
│  │     转换为 ToolSpec   │                         │
│  │                      │                         │
│  │  3. 执行工具调用时    │                         │
│  │     委托给 MCP Client │──── MCP Client ────► MCP Server
│  │                      │     tools/call          │
│  └──────────────────────┘                         │
└───────────────────────────────────────────────────┘
```

**支持多个 MCP Client**：你可以连接多个 MCP Server，它们的工具会自动聚合：

```java
ToolProvider toolProvider = McpToolProvider.builder()
        .mcpClients(List.of(
            filesystemClient,   // 文件系统工具
            databaseClient,     // 数据库工具
            baiduMapClient      // 百度地图工具
        ))
        .build();

// AI Service 将同时拥有以上所有工具！
```

#### 🧹 Finally 块：确保资源清理

```java
try {
    // ... 对话逻辑
} finally {
    mcpClient.close();
}
```

`mcpClient.close()` 对于 Stdio 传输至关重要：

1. 向子进程发送关闭信号
2. 如果子进程未响应，调用 `destroyForcibly()` 强制终止
3. 关闭 stdin/stdout/stderr 管道

**如果不调用 `close()`**：子进程会变为僵尸进程，持续占用系统资源。

---

## 五、实战案例二：基于 Stdio 的百度地图查询助手

第二个案例展示如何通过社区 MCP Server 接入外部 API 服务——百度地图。这个案例来自 Spring Boot 模块 `langchain4j-spring-boot-10-mcp`。

### 5.1 社区 MCP Server 生态

除了官方 Server，社区贡献了大量的 MCP Server，覆盖各种服务：

| MCP Server | 功能 | 安装方式 |
|------------|------|----------|
| `@baidumap/mcp-server-baidu-map` | 百度地图（地理编码、逆地理编码、天气查询） | `npx -y @baidumap/mcp-server-baidu-map` |
| `@modelcontextprotocol/server-filesystem` | 文件系统操作 | `npx -y @modelcontextprotocol/server-filesystem` |
| `@modelcontextprotocol/server-postgres` | PostgreSQL 数据库 | `npx -y @modelcontextprotocol/server-postgres` |
| `@modelcontextprotocol/server-github` | GitHub API | 官方维护 |
| `@anthropic/mcp-server-brave-search` | Brave 搜索引擎 | 官方维护 |
| `@modelcontextprotocol/server-everything` | 测试/参考实现 | 官方维护 |

### 5.2 Spring Boot 集成实现

在 Spring Boot 项目中，ChatModel 由 Spring 容器管理，我们通过 `@Autowired` 注入即可。

**定义 Assistant 接口**（Spring Boot 方式）：

```java
package com.langchain4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel"
)
public interface Assistant {

    @SystemMessage("你是一个AI智能助手")
    String chat(String message);
}
```

**测试代码**：

```java
@SpringBootTest
public class McpTest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Test
    public void testStudioBaiduMap() throws Exception {
        // Step 1: 构建 Stdio 传输层
        McpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of(
                    "D:/Program Files/nodejs/npx.cmd",  // npx 绝对路径
                    "-y",
                    "@baidumap/mcp-server-baidu-map"
                ))
                .environment(Map.of(
                    "BAIDU_MAP_API_KEY", "your_api_key_here"
                ))
                .logEvents(true)
                .build();

        // Step 2-4: 创建 MCP Client → ToolProvider → AI Service
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(openAiChatModel)
                .toolProvider(toolProvider)
                .build();

        // Step 5: 对话
        String response = assistant.chat("查询一下深圳今天的天气");
        System.out.println("response: " + response);

        mcpClient.close();
    }
}
```

### 5.3 环境变量传递

注意这个关键配置：

```java
.environment(Map.of(
    "BAIDU_MAP_API_KEY", "your_api_key_here"
))
```

`.environment()` 方法允许你向 MCP Server 子进程传递环境变量。这是一个重要的安全设计：

```
为什么通过环境变量传递 API Key？
        │
        ▼
┌──────────────────────────────────────────┐
│  1. 不在命令行参数中暴露（避免 ps -ef 泄露）│
│  2. 不在代码中硬编码（避免提交到 Git）      │
│  3. MCP Server 子进程的环境变量是隔离的     │
│     （不影响父进程的环境变量）              │
└──────────────────────────────────────────┘
```

**生产环境最佳实践**：

```java
// ❌ 不推荐：硬编码 API Key
.environment(Map.of("BAIDU_MAP_API_KEY", "dD2T4TUrZgl4OmmqkjGhuV1iNqggrYP3"))

// ✅ 推荐：从环境变量或配置中心读取
.environment(Map.of("BAIDU_MAP_API_KEY",
    System.getenv("BAIDU_MAP_API_KEY")))

// ✅ 更推荐：使用 Spring 配置
@Value("${baidu.map.api-key}")
private String baiduMapApiKey;
// ...
.environment(Map.of("BAIDU_MAP_API_KEY", baiduMapApiKey))
```

---

## 六、实战案例三：基于 HTTP/SSE 的远程工具调用

第三个案例展示 HTTP/SSE 传输模式。这种方式适合调用远程部署的 MCP Server。

### 6.1 前置准备

首先需要启动一个 HTTP 模式的 MCP Server。以官方 `everything` 测试服务器为例：

```bash
# 克隆官方的 servers 仓库
git clone https://github.com/modelcontextprotocol/servers.git
cd servers/src/everything

# 安装依赖并启动（监听 localhost:3001）
npm install && node dist/sse.js
```

这个 `everything` Server 是一个参考实现，提供了 `add`、`echo` 等示例工具，非常适合开发和测试。

### 6.2 完整实现

```java
package dev.langchain4j.example.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;

import java.time.Duration;
import java.util.List;

public class McpToolsExampleOverHttp {

    public static void main(String[] args) throws Exception {

        // ============ Step 1: 配置 ChatModel ============
        ChatModel model = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .logRequests(true)
                .logResponses(true)
                .build();

        // ============ Step 2: 创建 HTTP 传输层 ============
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:3001/sse")    // MCP Server 的 SSE 端点
                .timeout(Duration.ofSeconds(60))         // 工具执行超时
                .logRequests(true)                       // 调试日志
                .logResponses(true)
                .build();

        // ============ Step 3-5: MCP Client → ToolProvider → AI Service ============
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        Bot bot = AiServices.builder(Bot.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .build();

        // ============ Step 6: 对话 ============
        try {
            String response = bot.chat(
                "What is 5+12? Use the provided tool to answer " +
                "and always assume that the tool is correct."
            );
            System.out.println(response);
        } finally {
            mcpClient.close();
        }
    }
}
```

### 6.3 HTTP vs Stdio 的关键差异

```java
// Stdio 方式 —— 子进程通信
McpTransport transport = new StdioMcpTransport.Builder()
        .command(List.of("npx", "-y", "server-package"))
        .environment(Map.of("KEY", "VALUE"))   // 通过环境变量传递密钥
        .build();

// HTTP 方式 —— 网络通信
McpTransport transport = new HttpMcpTransport.Builder()
        .sseUrl("http://localhost:3001/sse")   // 远程 SSE 端点
        .timeout(Duration.ofSeconds(60))        // 超时控制
        .build();
```

**HTTP 方式特有的考量**：

| 考量 | 说明 |
|------|------|
| **超时控制** | 工具执行可能很慢（如大数据查询），需要设置合理的超时 |
| **认证** | 远程 Server 需要认证机制（API Key / OAuth / mTLS） |
| **断线重连** | SSE 连接可能断开，需要考虑重连策略 |
| **负载均衡** | 多个 Client 共享同一 Server，需要考虑并发能力 |
| **TLS** | 生产环境建议使用 HTTPS |

---

## 七、MCP 安全模型深度解析

MCP 协议从设计之初就将安全放在核心位置。下面我们深入分析 MCP 的安全模型。

### 7.1 白名单机制（目录级沙箱）

这是 `@modelcontextprotocol/server-filesystem` 的安全核心：

```
MCP Server 启动命令：
  npx server-filesystem /allowed/path

白名单目录：/allowed/path

访问控制矩阵：

请求路径                        允许？    原因
─────────────────────────────────────────────────
/allowed/path/file.txt           ✅      在白名单内
/allowed/path/sub/deep/data.md   ✅      白名单的子目录
/allowed/path/../other/file      ❌      试图跳出白名单
/etc/passwd                      ❌      绝对路径不在白名单
~/secret.key                     ❌      home 目录不在白名单
../../root/file                  ❌      相对路径跳出
```

**实现原理**（伪代码）：

```java
// MCP Server 内部的文件访问校验逻辑
String resolvePath(String requested) {
    Path allowed = Path.of("/allowed/path").toRealPath();
    Path target = allowed.resolve(requested).toRealPath();

    if (!target.startsWith(allowed)) {
        throw new SecurityException(
            "Access denied: " + requested + " is outside allowed directory"
        );
    }
    return target.toString();
}
```

### 7.2 环境变量隔离

```
┌─────────────────────────────────────────┐
│          父进程 (Java)                     │
│  ┌─────────────────────────────────┐    │
│  │ 环境变量：                        │    │
│  │   PATH=/usr/bin                  │    │
│  │   JAVA_HOME=/opt/jdk17           │    │
│  │   DATABASE_PASSWORD=secret123    │    │  ← MCP Server 看不到！
│  │   OPENAI_API_KEY=sk-xxx          │    │  ← MCP Server 看不到！
│  └─────────────────────────────────┘    │
│              │                           │
│              │ ProcessBuilder            │
│              │ .environment(customEnv)   │
│              ▼                           │
│  ┌─────────────────────────────────┐    │
│  │  子进程 (MCP Server)              │    │
│  │  环境变量：                        │    │
│  │   BAIDU_MAP_API_KEY=xxx          │    │  ← 仅此一个！
│  │   （仅包含显式传递的变量）           │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

**这意味着**：
- MCP Server 子进程**看不到**父进程的环境变量（除非显式传递）
- 你的数据库密码、API Key 等敏感信息不会泄露给 MCP Server
- 每个 MCP Server 只能访问它需要的特定密钥

### 7.3 工具级权限控制

LangChain4j 的 `McpToolProvider` 不是"全有或全无"的。你可以通过 `ToolProvider` 的过滤机制实现工具级别的权限控制：

```java
// 获取 MCP Server 暴露的所有工具
ToolProvider rawProvider = McpToolProvider.builder()
        .mcpClients(List.of(mcpClient))
        .build();

// 只允许使用安全的工具
ToolProvider filteredProvider = ToolProvider.builder()
        .tools(rawProvider.getTools().stream()
                .filter(tool -> isAllowed(tool.name()))
                .toList())
        .build();

// 或者只允许特定前缀的工具
private boolean isAllowed(String toolName) {
    return toolName.startsWith("read_")  // 只允许读取，不允许写入
        || toolName.equals("echo");      // 测试工具
}
```

### 7.4 审计日志

开启 `logEvents(true)` 可以完整记录 MCP 通信：

```
[STDIO] → tools/list             ← 获取工具列表
[STDIO] ← 返回 3 个可用工具
[STDIO] → tools/call read_file   ← 调用读文件工具
          path=/allowed/file.txt
[STDIO] ← 返回文件内容 200 字节
[STDIO] → tools/call write_file  ← 试图写文件
[STDIO] ← 错误：write_file 不在工具列表中
```

这些日志可用于：
- 📊 **安全审计**：追踪 AI 调用了哪些工具、操作了什么资源
- 🔍 **问题排查**：当 AI 行为异常时回溯工具调用链
- 📈 **使用分析**：统计哪些工具最常被调用、哪些参数组合最常见

### 7.5 安全最佳实践清单

| 安全维度 | 最佳实践 |
|----------|----------|
| **白名单范围** | 给出**最小必要**的目录/资源访问范围 |
| **API Key 管理** | 通过环境变量传递，不硬编码、不传命令行参数 |
| **工具过滤** | 只暴露 AI 真正需要的工具（只读 vs 读写） |
| **超时限制** | 设置合理的工具执行超时，防止无限等待 |
| **资源限制** | 限制 MCP Server 子进程的 CPU/内存使用 |
| **日志审计** | 生产环境记录工具调用日志，定期审查 |
| **版本锁定** | 使用精确版本号（如 `@0.6.2`），避免自动升级引入风险 |
| **网络隔离** | Stdio 模式天然网络隔离；HTTP 模式建议内网部署 |

---

## 八、MCP 生态与社区

### 8.1 官方 MCP Server 列表

Anthropic 官方维护了一系列 MCP Server 参考实现：

```
https://github.com/modelcontextprotocol/servers
```

| Server | 功能 | 工具示例 |
|--------|------|----------|
| **server-filesystem** | 文件系统操作 | `read_file`, `write_file`, `list_directory` |
| **server-postgres** | PostgreSQL 数据库 | `execute_sql`, `list_tables`, `describe_table` |
| **server-github** | GitHub API | `create_issue`, `search_code`, `get_pr` |
| **server-brave-search** | 搜索引擎 | `web_search`, `local_search` |
| **server-everything** | 测试/参考 | `add`, `echo`, `longRunningOperation` |
| **server-slack** | Slack 集成 | `send_message`, `list_channels` |
| **server-memory** | 持久化记忆 | `create_entities`, `search_nodes` |

### 8.2 社区 MCP Server

社区贡献了大量第三方 MCP Server，可在以下平台发现：

- **npm 搜索**：`@modelcontextprotocol/server-*` 或 `mcp-server-*`
- **GitHub Topics**：搜索 `mcp-server` 标签
- **MCP Hub**：社区维护的 MCP Server 索引

### 8.3 自己搭建 MCP Server

如果你需要封装自己的工具为 MCP Server，LangChain4j 也支持构建 MCP Server 端。核心思路：

1. 使用 `langchain4j-mcp` 模块的 Server 端 API
2. 定义你的工具和资源
3. 选择 Stdio 或 HTTP 传输方式
4. 让任何 MCP Client（Java、Python、TypeScript...）消费你的工具

这实现了真正的"一次编写，到处调用"。

---

## 九、常见问题与避坑指南

### 9.1 Windows 上 npx 无法执行

**错误现象**：
```
java.io.IOException: Cannot run program "npx": CreateProcess error=2
```

**原因**：Windows 上 `npx` 是 `.cmd` 批处理文件，Java 无法直接执行。

**解决方案**：
```java
if (isWindows) {
    command.add("cmd");
    command.add("/c");
    command.add("npx");
} else {
    command.add("npx");
}
```

或者使用 npx 的绝对路径：
```java
command.add("D:/Program Files/nodejs/npx.cmd");
```

### 9.2 工作目录不正确导致路径错误

**错误现象**：
```
MCP Server 报告：File not found: src/main/resources/file.txt
```

**原因**：IDE 或 Maven 的运行工作目录可能不是项目根目录。

**解决方案**：
```java
// 使用绝对路径
String absolutePath = new File("src/main/resources")
        .getAbsolutePath();
command.add(absolutePath);

// 或显式设置工作目录
ProcessBuilder pb = new ProcessBuilder(command);
pb.directory(new File("/correct/working/dir"));
```

### 9.3 模型不支持 Function Calling

**错误现象**：AI 不调用工具，而是自行回答。

**原因**：某些模型（如早期的文本生成模型）不支持 Function Calling。

**解决方案**：
- ✅ 使用 **GPT-4o / GPT-4o-mini**（最可靠）
- ✅ 使用 **DeepSeek-chat**（支持 Function Calling）
- ⚠️ 避免使用纯文本模型或超轻量模型

**验证方法**：
```java
// 在提示词中明确引导模型使用工具
String response = bot.chat(
    "What is 5+12? Use the provided tool to answer " +
    "and always assume that the tool is correct."
);
```

### 9.4 子进程成为僵尸进程

**错误现象**：程序退出后，MCP Server 进程仍然在后台运行。

**原因**：没有在 `finally` 块中调用 `mcpClient.close()`。

**解决方案**：
```java
McpClient mcpClient = null;
try {
    mcpClient = new DefaultMcpClient.Builder()
            .transport(transport)
            .build();
    // ... 使用 mcpClient
} finally {
    if (mcpClient != null) {
        mcpClient.close();  // 保证无论是否异常都清理子进程
    }
}

// 或在 Spring Boot 中将 McpClient 注册为 Bean 并设置 destroyMethod
@Bean(destroyMethod = "close")
public McpClient mcpClient() { ... }
```

### 9.5 HTTP 传输的 SSE 连接超时

**错误现象**：
```
java.util.concurrent.TimeoutException: SSE connection timeout
```

**原因**：工具执行时间过长，超过了客户端设置的超时时间。

**解决方案**：
```java
// 增加超时时间
.timeout(Duration.ofSeconds(120))  // 2 分钟

// 或在 Server 端优化工具执行效率
// 对于长时间操作，采用异步模式
```

---

## 十、进阶技巧与最佳实践

### 10.1 多 MCP Server 聚合

当你的应用需要集成多个外部工具源时：

```java
// 文件系统 Server
McpClient filesystemClient = new DefaultMcpClient.Builder()
        .transport(new StdioMcpTransport.Builder()
            .command(filesystemCommand)
            .build())
        .build();

// 数据库 Server
McpClient databaseClient = new DefaultMcpClient.Builder()
        .transport(new StdioMcpTransport.Builder()
            .command(databaseCommand)
            .build())
        .build();

// 百度地图 Server
McpClient baiduMapClient = new DefaultMcpClient.Builder()
        .transport(new StdioMcpTransport.Builder()
            .command(baiduMapCommand)
            .environment(Map.of("BAIDU_MAP_API_KEY", apiKey))
            .build())
        .build();

// 聚合所有工具
ToolProvider unifiedProvider = McpToolProvider.builder()
        .mcpClients(List.of(
            filesystemClient,
            databaseClient,
            baiduMapClient
        ))
        .build();

// AI Service 现在拥有 3 个 MCP Server 的所有工具！
Bot bot = AiServices.builder(Bot.class)
        .chatModel(model)
        .toolProvider(unifiedProvider)
        .build();
```

### 10.2 MCP 与 @Tool 混合使用

MCP 工具和本地 `@Tool` 可以并存：

```java
public class HybridAssistant {

    // 本地 @Tool：访问内部业务数据库
    @Tool("查询用户的订单列表")
    public List<Order> queryOrders(@P("用户ID") String userId) {
        return orderService.findByUserId(userId);
    }

    @Tool("查询用户信息")
    public User getUserInfo(@P("用户ID") String userId) {
        return userService.findById(userId);
    }
}

// 构建时同时注入本地工具和 MCP 工具
HybridAssistant assistant = new HybridAssistant();
Bot bot = AiServices.builder(Bot.class)
        .chatModel(model)
        .tools(assistant)              // 本地 @Tool
        .toolProvider(mcpToolProvider) // MCP 远程工具
        .build();
```

这种混合模式非常实用：
- 📌 **本地 @Tool**：处理内部业务逻辑、数据库操作
- ✅ **MCP**：接入外部服务、第三方 API、跨语言工具

### 10.3 Spring Boot 环境下的 McpClient 生命周期管理

```java
@Configuration
public class McpConfiguration {

    @Value("${npx.path}")
    private String npxPath;

    @Bean(destroyMethod = "close")
    public McpClient fileSystemMcpClient() {
        List<String> command = List.of(
            npxPath, "-y",
            "@modelcontextprotocol/server-filesystem@0.6.2",
            "/allowed/directory"
        );

        McpTransport transport = new StdioMcpTransport.Builder()
                .command(command)
                .build();

        return new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    @Bean
    public ToolProvider mcpToolProvider(List<McpClient> mcpClients) {
        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .build();
    }
}
```

利用 Spring 的 `@Bean(destroyMethod = "close")` 确保应用关闭时自动清理 MCP 子进程。

### 10.4 错误处理与降级

```java
public String chatWithFallback(String prompt) {
    try {
        return bot.chat(prompt);  // 尝试使用 MCP 工具
    } catch (Exception e) {
        logger.warn("MCP 工具调用失败，降级到纯文本回答", e);

        // 降级：不带工具的纯对话
        Bot botWithoutTools = AiServices.builder(Bot.class)
                .chatModel(model)
                .build();
        return botWithoutTools.chat(
            prompt + "\n（注意：工具服务暂时不可用，请基于你的知识回答）"
        );
    }
}
```

---

## 十一、总结与展望

### 本文核心要点回顾

| 知识点 | 核心内容 |
|--------|----------|
| **MCP 定位** | 标准化 LLM 与外部工具/数据源的通信协议 |
| **两大传输** | Stdio（子进程通信，适合本地工具）vs HTTP/SSE（网络通信，适合远程服务） |
| **核心 API** | `StdioMcpTransport` / `HttpMcpTransport` → `DefaultMcpClient` → `McpToolProvider` |
| **安全设计** | 白名单目录、环境变量隔离、工具级权限过滤、审计日志 |
| **MCP vs @Tool** | 跨语言复用 → MCP；简单本地工具 → @Tool |
| **生态现状** | 官方 7+ Server + 社区大量第三方 Server |

### 适用场景速查

```
你的需求                                      推荐方案
─────────────────────────────────────────────────────
集成已有的 Python/TS 工具服务                    → MCP
使用社区现成的 MCP Server（地图、搜索等）         → MCP
多个 Java 应用共享同一组工具                     → MCP
简单的本地 Java 方法暴露给 AI                    → @Tool
仅在本应用内使用的业务逻辑                       → @Tool
需要跨语言、跨平台复用                           → MCP
```

### 下一篇预告

在下一篇（第 30 篇）中，我们将进入**第六阶段：生产级项目实战**，第一个实战项目是 **Spring Boot + MySQL + Thymeleaf 失物招领系统全栈开发**。我们将从数据库设计、REST API 实现到前端模板渲染，手把手构建一个完整的 Web 应用。敬请期待！

---

## 延伸阅读

- [MCP 协议官方规范](https://modelcontextprotocol.io/)
- [MCP 官方 GitHub 仓库](https://github.com/modelcontextprotocol)
- [MCP Server 官方列表](https://github.com/modelcontextprotocol/servers)
- [LangChain4j MCP 模块文档](https://docs.langchain4j.dev/)
- [Anthropic 官方 MCP 介绍文章](https://www.anthropic.com/news/model-context-protocol)
- [第 11 篇：Function Calling 工具调用](../第11篇-FunctionCalling工具调用.md)
- [第 25 篇：A2A 协议 —— Agent 之间的通信与协作](../第25篇-A2A协议.md)

---

> 📝 **代码位置**：
> - 纯 Java 示例：`langchain4j-04-mcp-examples/src/main/java/dev/langchain4j/example/mcp/`
> - Spring Boot 示例：`langchain4j-spring-boot-10-mcp/src/test/java/com/langchain4j/McpTest.java`
>
> 💡 **运行提示**：运行 MCP 示例前请确保已安装 Node.js + npm，并且正确配置了 API Key。

---

**发布时间**：2026-06-08  
**专栏名称**：《LangChain4j Java AI 应用开发实战》  
**代码版本**：LangChain4j 1.14.0  
**作者**：LangChain4j Cookbook 项目组
