package com.langchain4j;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;


@SpringBootTest
public class McpTest {
    @Autowired
    private OpenAiStreamingChatModel openAiStreamingChatModel;

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Test
    public void testStudioBaiduMap() throws Exception{
        McpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of("D:/Program Files/nodejs/npx.cmd", "-y",
                        "@baidumap/mcp-server-baidu-map"))
                .environment(Map.of("BAIDU_MAP_API_KEY","dD2T4TUrZgl4OmmqkjGhuV1iNqggrYP3"))
                .logEvents( true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        Assistant assistant=AiServices.builder(Assistant.class)
                .chatModel(openAiChatModel)
                .toolProvider(toolProvider)
                .build();

        String response = assistant.chat("查询一下深圳今天的天气");
        System.out.println("response: " + response);
        mcpClient.close();
    }
    @Test
    public void testStudioSystemFile() throws Exception{
        McpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of("D:/Program Files/nodejs/npm.cmd", "exec",
                        "@modelcontextprotocol/server-filesystem@0.6.2",
                        new File("src/main/resources").getAbsolutePath()))
                .logEvents( true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        Assistant assistant=AiServices.builder(Assistant.class)
                .chatModel(openAiChatModel)
                .toolProvider(toolProvider)
                .build();

        try {
            File file = new File("D:\\github\\langchain4j-cookbook\\langchain4j-spring-boot-10-mcp\\src\\main\\resources\\test.txt");
            String response = assistant.chat("请读取这个文件的内容 " + file.getAbsolutePath());
            System.out.println("RESPONSE: " + response);
        } finally {
            mcpClient.close();
        }
    }

    @SuppressWarnings("removal")
    @Test
    public void testHttp() throws Exception{
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:3001/sse")
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        Assistant assistant=AiServices.builder(Assistant.class)
                .chatModel(openAiChatModel)
                .toolProvider(toolProvider)
                .build();
        try {
            String response = assistant.chat("请使用这个工具回答问题XXXXX");
            System.out.println(response);
        } finally {
            mcpClient.close();
        }
    }
}
