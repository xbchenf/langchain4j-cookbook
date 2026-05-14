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

/**
 * MCP（Model Context Protocol）工具调用示例 —— Stdio（标准输入输出）传输方式
 *
 * 本示例演示 LangChain4j 如何通过 stdio 方式启动并连接 MCP 服务器，
 * 让大语言模型安全地访问本地文件系统。
 *
 * 核心机制：
 * - Java 进程通过 Runtime.exec 启动 MCP 服务器作为子进程
 * - 通过子进程的 stdin 发送 MCP 协议消息
 * - 通过子进程的 stdout 接收 MCP 协议响应
 * - 这种方式无需网络端口，适合本地工具（如文件系统、数据库、命令行工具）
 *
 * 前置条件：
 * 1. 系统已安装 Node.js 和 npm
 * 2. 项目根目录下存在 src/main/resources/file.txt（示例要读取的文件）
 * 3. 工作目录必须为项目根目录（langchain4j-examples/mcp-example），否则路径会错位
 */
public class McpToolsExampleOverStdio {

    // AI 将要读取的目标文件路径（相对路径，依赖于工作目录）
    public static final String FILE_TO_BE_READ = "src/main/resources/file.txt";

    /**
     * 本示例使用官方 MCP 服务器 @modelcontextprotocol/server-filesystem，
     * 它提供了一组文件系统操作工具（read_file, list_directory 等）。
     *
     * 运行前请确保：
     * - npm 已安装且可在命令行调用
     * - 工作目录是项目根目录，否则 "src/main/resources" 路径会失效
     *
     * 通信方式：stdio（标准输入输出），无需网络端口，子进程直接通信。
     */
    public static void main(String[] args) throws Exception {

        // ==================== 1. 配置大语言模型 ====================

        ChatModel model = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
//                .logRequests(true)
//                .logResponses(true)
                .build();

        // ==================== 2. 配置 Stdio MCP 传输层 ====================

        /**
         * 创建 Stdio MCP 传输层，通过启动子进程与 MCP 服务器通信。
         *
         * 命令拆解：
         * - /usr/bin/npm: npm 可执行文件路径
         * - exec: npm 的子命令，临时安装并执行包
         * - @modelcontextprotocol/server-filesystem@0.6.2: 具体的 MCP 服务器包及版本
         * - new File("src/main/resources").getAbsolutePath(): 允许服务器访问的目录（白名单机制）
         *
         * ⚠️ 问题 2：/usr/bin/npm 是硬编码的 Unix 路径。
         * - macOS/Linux 上可能有效，但 npm 也可能在 /usr/local/bin/npm
         * - Windows 上路径完全不同，会直接报错
         * 修复建议：使用 "npm" 让系统从 PATH 中查找，或根据 OS 动态判断。
         *
         * ⚠️ 问题 3：new File("src/main/resources") 依赖工作目录。
         * 如果 IDE 或命令行的工作目录不是项目根目录，路径会指向错误位置，
         * MCP 服务器将找不到文件，或访问被拒绝。
         * 修复建议：使用基于类路径的方式定位，或启动时校验工作目录。
         */
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        List<String> command = new ArrayList<>();
        if (isWindows) {
            // Windows: 必须用 cmd /c 才能执行 .cmd 脚本
            command.add("cmd");
            command.add("/c");
            command.add("npx");
        } else {
            // macOS / Linux
            command.add("npx");
        }

        command.add("-y");
        command.add("@modelcontextprotocol/server-filesystem@0.6.2");
        command.add(new File("src/main/resources").getAbsolutePath());

        McpTransport transport = new StdioMcpTransport.Builder()
                .command(command)
                .logEvents(true) // 打印 stdio 通信事件（调试用）
                .build();


        // ==================== 3. 创建 MCP 客户端 ====================

        /**
         * 创建默认 MCP 客户端，绑定 stdio 传输层。
         *
         * 此时 StdioMcpTransport 会在内部启动子进程：
         * Runtime.exec([npm, exec, server-filesystem, /absolute/path/to/resources])
         *
         * 如果 npm 未安装、包下载失败或路径错误，这里会抛出 IOException。
         */
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        // ==================== 4. 包装为 ToolProvider ====================

        /**
         * 将 MCP 客户端包装为 LangChain4j 的 ToolProvider。
         * 使得 AiServices 可以动态发现 MCP 服务器提供的文件系统工具。
         */
        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        // ==================== 5. 构建 AI Service ====================

        /**
         * 构建 Bot 代理，绑定模型和远程文件系统工具。
         *
         * 此时 Bot 具备的能力：
         * - 自然语言对话
         * - 通过 MCP 调用 server-filesystem 的 read_file、list_directory 等工具
         */
        Bot bot = AiServices.builder(Bot.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .build();

        // ==================== 6. 执行对话：让 AI 读取文件 ====================

        /**
         * 构造绝对路径，让 AI 读取指定文件内容。
         *
         * ⚠️ 问题 4：没有校验 file.txt 是否存在。
         * 如果文件不存在，AI 调用 read_file 工具后会收到错误信息，
         * 然后向用户报告文件不存在，体验较差。
         * 修复建议：提前检查 file.exists()。
         */
        try {
            File file = new File(FILE_TO_BE_READ);

            // 建议添加前置校验：
            // if (!file.exists()) {
            //     System.err.println("文件不存在: " + file.getAbsolutePath());
            //     return;
            // }

            String response = bot.chat("Read the contents of the file " + file.getAbsolutePath());
            System.out.println("RESPONSE: " + response);
        } finally {
            /**
             * 关闭 MCP 客户端。
             *
             * 对于 StdioMcpTransport，close() 会：
             * 1. 向子进程发送关闭信号
             * 2. 销毁子进程（destroyForcibly 如果必要）
             * 3. 关闭 stdin/stdout 管道
             *
             * 放在 finally 中确保子进程不会成为僵尸进程。
             */
            mcpClient.close();
        }
    }
}