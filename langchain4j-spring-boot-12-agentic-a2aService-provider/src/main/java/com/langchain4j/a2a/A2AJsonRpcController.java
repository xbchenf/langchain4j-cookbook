package com.langchain4j.a2a;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.spec.Artifact;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A2A JSON-RPC 协议控制器
 *
 * 处理 A2A 客户端的所有 JSON-RPC 请求，是 A2A 服务端的核心入口。
 *
 * <h3>协议端点</h3>
 * <ul>
 *   <li>GET /.well-known/agent-card.json — Agent 发现（AgentCard）</li>
 *   <li>POST / — JSON-RPC 2.0 请求处理</li>
 * </ul>
 *
 * <h3>支持的 JSON-RPC 方法</h3>
 * <ul>
 *   <li>tasks/send — 发送消息给 Agent，触发故事创作</li>
 *   <li>tasks/get — 查询任务状态</li>
 *   <li>tasks/cancel — 取消进行中的任务</li>
 * </ul>
 */
@RestController
public class A2AJsonRpcController {

    private static final Logger log = LoggerFactory.getLogger(A2AJsonRpcController.class);

    private final io.a2a.spec.AgentCard agentCard;
    private final StoryWriterService storyWriter;
    private final ObjectMapper objectMapper;

    /** 任务存储（生产环境应使用数据库或分布式缓存） */
    private final Map<String, Task> taskStore = new ConcurrentHashMap<>();

    private final AtomicLong taskIdCounter = new AtomicLong(1);

    public A2AJsonRpcController(io.a2a.spec.AgentCard agentCard,
                                StoryWriterService storyWriter,
                                ObjectMapper objectMapper) {
        this.agentCard = agentCard;
        this.storyWriter = storyWriter;
        this.objectMapper = objectMapper;
    }

    /**
     * AgentCard 发现端点
     *
     * A2A 客户端首次连接时，通过此端点获取 Agent 的元数据，
     * 包括名称、能力、技能列表、传输协议等。
     */
    @GetMapping("/.well-known/agent-card.json")
    public io.a2a.spec.AgentCard getAgentCard() {
        return agentCard;
    }

    /**
     * JSON-RPC 2.0 请求处理入口
     *
     * 解析请求中的 method 字段，路由到对应的处理方法。
     * 支持通知（无 id 字段时不返回响应）。
     */
    @PostMapping("/")
    public ResponseEntity<?> handleJsonRpc(@RequestBody JsonNode body) {
        String method = body.has("method") ? body.get("method").asText() : null;
        JsonNode idNode = body.get("id");

        if (method == null) {
            return jsonRpcError(idNode, -32600, "Missing 'method' field");
        }

        try {
            return switch (method) {
                case "tasks/send", "message/send" -> handleTaskSend(body, idNode);
                case "tasks/get", "tasks/pushNotificationConfig/get" -> handleTaskGet(body, idNode);
                case "tasks/cancel" -> handleTaskCancel(body, idNode);
                case "agent/getCard" -> handleAgentGetCard(body, idNode);
                default -> jsonRpcError(idNode, -32601, "Method not found: " + method);
            };
        } catch (Exception e) {
            log.error("Error handling A2A request: {}", method, e);
            return jsonRpcError(idNode, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * 处理 tasks/send — 将消息发送给 Agent 执行
     *
     * 从消息中提取文本内容，调用 AI 服务生成故事，
     * 将结果打包为 Task 中的 Artifact 返回。
     */
    private ResponseEntity<Map<String, Object>> handleTaskSend(JsonNode body, JsonNode idNode) {
        JsonNode params = body.get("params");
        if (params == null) {
            return jsonRpcError(idNode, -32602, "Missing 'params' field");
        }

        String taskId = params.has("id") ? params.get("id").asText()
                : "task-" + taskIdCounter.getAndIncrement();

        // 提取消息文本：遍历 parts，拼接所有 text 类型的内容
        JsonNode message = params.get("message");
        String userText = "";
        if (message != null && message.has("parts")) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : message.get("parts")) {
                // A2A 0.3.x 使用 kind 字段，1.0.x 使用 type 字段
                String partType = part.has("kind") ? part.get("kind").asText()
                        : part.has("type") ? part.get("type").asText() : "";
                if (("text".equals(partType) || part.has("text")) && part.has("text")) {
                    sb.append(part.get("text").asText());
                }
            }
            userText = sb.toString();
        }

        log.info("A2A tasks/send — taskId: {}, topic: {}", taskId, userText);

        // 调用 AI 服务生成故事
        String story = storyWriter.writeStory(userText);

        // 构建 Task 响应
        Task task = new Task.Builder()
                .id(taskId)
                .contextId(UUID.randomUUID().toString())
                .status(new TaskStatus(TaskState.COMPLETED))
                .artifacts(List.of(
                        new Artifact.Builder()
                                .artifactId(UUID.randomUUID().toString())
                                .name("创作的故事")
                                .parts(new TextPart(story))
                                .build()
                ))
                .history(new ArrayList<>())
                .build();

        taskStore.put(taskId, task);

        return jsonRpcResponse(idNode, task);
    }

    /**
     * 处理 tasks/get — 查询任务状态
     */
    private ResponseEntity<Map<String, Object>> handleTaskGet(JsonNode body, JsonNode idNode) {
        JsonNode params = body.get("params");
        if (params == null || !params.has("id")) {
            return jsonRpcError(idNode, -32602, "Missing 'params.id' field");
        }

        String taskId = params.get("id").asText();
        Task task = taskStore.get(taskId);

        if (task == null) {
            return jsonRpcError(idNode, -32000, "Task not found: " + taskId);
        }

        return jsonRpcResponse(idNode, task);
    }

    /**
     * 处理 tasks/cancel — 取消任务
     */
    private ResponseEntity<Map<String, Object>> handleTaskCancel(JsonNode body, JsonNode idNode) {
        JsonNode params = body.get("params");
        if (params == null || !params.has("id")) {
            return jsonRpcError(idNode, -32602, "Missing 'params.id' field");
        }

        String taskId = params.get("id").asText();
        Task existingTask = taskStore.get(taskId);

        if (existingTask == null) {
            return jsonRpcError(idNode, -32000, "Task not found: " + taskId);
        }

        TaskState currentState = existingTask.getStatus().state();
        if (currentState == TaskState.CANCELED || currentState == TaskState.COMPLETED) {
            return jsonRpcError(idNode, -32001, "Task not cancelable in state: " + currentState.asString());
        }

        Task canceledTask = new Task.Builder(existingTask)
                .status(new TaskStatus(TaskState.CANCELED))
                .build();
        taskStore.put(taskId, canceledTask);

        return jsonRpcResponse(idNode, canceledTask);
    }

    /**
     * 处理 agent/getCard — JSON-RPC 方式获取 AgentCard
     */
    private ResponseEntity<Map<String, Object>> handleAgentGetCard(JsonNode body, JsonNode idNode) {
        return jsonRpcResponse(idNode, agentCard);
    }

    // ==================== 响应构建辅助方法 ====================

    /**
     * 构建 JSON-RPC 成功响应
     */
    private ResponseEntity<Map<String, Object>> jsonRpcResponse(JsonNode idNode, Object result) {
        String jsonrpcId = idNode != null ? idNode.asText() : "0";
        return ResponseEntity.ok(Map.of(
                "jsonrpc", "2.0",
                "result", result,
                "id", jsonrpcId
        ));
    }

    /**
     * 构建 JSON-RPC 错误响应
     *
     * @param idNode  请求中的 id 字段
     * @param code    JSON-RPC 错误码
     * @param message 错误描述
     */
    private ResponseEntity<Map<String, Object>> jsonRpcError(JsonNode idNode, int code, String message) {
        String jsonrpcId = idNode != null ? idNode.asText() : "0";
        log.warn("A2A JSON-RPC error — code: {}, message: {}", code, message);
        return ResponseEntity.ok(Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", code, "message", message),
                "id", jsonrpcId
        ));
    }
}
