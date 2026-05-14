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

/**
 * MCP（Model Context Protocol）工具调用示例 —— HTTP/SSE 传输方式
 *
 * 本示例演示 LangChain4j 如何通过 MCP 协议连接外部工具服务器，
 * 让大语言模型动态发现并调用远程工具。
 *
 * MCP 是什么？
 * - Model Context Protocol，由 Anthropic 推出的开放协议
 * - 标准化 LLM 与外部数据源、工具之间的通信方式
 * - 类似"工具市场的 USB-C 接口"，统一了工具暴露和消费的方式
 *
 * 运行前置条件：
 * 1. 需要先启动 MCP 服务器（本例使用官方提供的 everything 示例服务器）
 *    参考：https://github.com/modelcontextprotocol/servers/tree/main/src/everything
 *    进入目录后执行：npm install && node dist/sse.js
 *    服务器启动后会监听 localhost:3001，提供 SSE 端点
 * 2. 确保 OpenAI 兼容端点可访问（本例使用 langchain4j 演示端点）
 */
public class McpToolsExampleOverHttp {

    public static void main(String[] args) throws Exception {

        // ==================== 1. 配置大语言模型 ====================

        /**
         * 创建 OpenAI 兼容的聊天模型。
         *
         * 本例使用 LangChain4j 官方演示端点，实际项目中应替换为真实 API Key 和地址。
         * modelName 指定 gpt-4o-mini，该模型支持 Function Calling，能理解和使用工具。
         */
        ChatModel model = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .logRequests(true)
                .logResponses(true)
                .build();

        // ==================== 2. 配置 MCP 传输层 ====================

        /**
         * 创建 HTTP MCP 传输层，使用 SSE（Server-Sent Events）协议与 MCP 服务器通信。
         *
         * SSE 是 MCP 协议支持的一种传输方式：
         * - 客户端通过 SSE 端点建立持久连接，接收服务器推送的消息
         * - 适合浏览器环境和无状态 HTTP 架构
         *
         * 参数说明：
         * - sseUrl: MCP 服务器的 SSE 端点地址
         * - timeout: 请求超时时间（60 秒），防止工具执行时间过长导致连接断开
         * - logRequests/logResponses: 开启通信日志（调试用，生产环境建议关闭）
         *
         * 注意：MCP 还支持 stdio（标准输入输出）传输方式，适合本地进程间通信。
         */
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:3001/sse")
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

        // ==================== 3. 创建 MCP 客户端 ====================

        /**
         * 创建默认 MCP 客户端，绑定上面配置的传输层。
         *
         * MCP 客户端负责：
         * - 与 MCP 服务器握手，获取服务器信息
         * - 发现并列出服务器提供的工具列表
         * - 将 LangChain4j 的工具调用请求序列化为 MCP 格式并发送
         * - 接收工具执行结果并返回
         *
         * 实现了 AutoCloseable，需要在 finally 中关闭以释放 HTTP 连接。
         */
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        // ==================== 4. 将 MCP 客户端包装为 ToolProvider ====================

        /**
         * McpToolProvider 是 LangChain4j 与 MCP 之间的桥梁。
         *
         * 它将一个或多个 MCP 客户端暴露的工具，统一转换为 LangChain4j 的 ToolProvider 接口。
         * 这样 AiServices 就能像使用本地 @Tool 注解一样，透明地调用远程 MCP 工具。
         *
         * 支持配置多个 MCP 客户端，实现工具聚合（多个服务器的工具合并到一个 Provider）。
         */
        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        // ==================== 5. 构建 AI Service ====================

        /**
         * 使用 AiServices 构建 Bot 代理。
         *
         * 与传统 @Tool 方式的区别：
         * - 传统方式：工具是本地 Java 方法（@Tool 注解）
         * - MCP 方式：工具来自远程 MCP 服务器，通过 ToolProvider 动态发现
         *
         * 运行时流程：
         * 1. AiServices 从 ToolProvider 获取可用工具列表（来自 MCP 服务器）
         * 2. 将工具描述发送给 LLM
         * 3. LLM 决定调用哪个工具（如 everything 服务器的 add 工具）
         * 4. AiServices 通过 McpToolProvider -> McpClient -> HttpMcpTransport 调用远程工具
         * 5. 拿到结果后回传给 LLM，生成最终回答
         */
        Bot bot = AiServices.builder(Bot.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .build();

        // ==================== 6. 调用对话并观察工具使用 ====================

        /**
         * 向 Bot 提问："5+12 等于多少？"
         *
         * 由于配置了 ToolProvider，模型知道有一个 add 工具可用，
         * 因此不会直接计算，而是发起工具调用请求。
         *
         * 提示词中要求"使用提供的工具回答，并始终假设工具是正确的"，
         * 这是为了引导模型优先使用工具而非自身计算能力。
         */
        try {
            String response = bot.chat("What is 5+12? Use the provided tool to answer " +
                    "and always assume that the tool is correct.");
            System.out.println(response);
        } finally {
            /**
             * 关闭 MCP 客户端，释放底层 HTTP 连接和 SSE 订阅资源。
             * 放在 finally 中确保即使对话异常也能正常清理。
             */
            mcpClient.close();
        }
    }
}