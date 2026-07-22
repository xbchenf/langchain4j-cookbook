package com.langchain4j.controller;

import com.langchain4j.aiagent.CustomerServiceAgent;
import com.langchain4j.config.ChatMemoryConfig;
import dev.langchain4j.data.message.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
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
 *
 * <h3>⚠️ 安全注意事项（教学项目）</h3>
 * 本 Controller 的 getChatHistory / clearChatHistory 接口直接接受 userId 参数，
 * 未做身份认证和授权检查——任何知道 userId 的请求方都可以读取或删除任意用户的聊天记录。
 * 这是典型的 IDOR（Insecure Direct Object Reference）漏洞。
 *
 * <p><b>当前设计属于教学简化</b>，不适用于生产环境。生产环境修复方案：</p>
 * <ol>
 *   <li>引入 Spring Security，添加认证中间件</li>
 *   <li>从 SecurityContextHolder 获取当前登录用户身份</li>
 *   <li>校验请求中的 userId 与当前登录用户一致，不一致则返回 403</li>
 *   <li>或直接从认证会话中推导 userId，不再接受前端传参</li>
 * </ol>
 *
 * <pre>{@code
 * // 生产环境修复示例：
 * @GetMapping("/chat-history")
 * @ResponseBody
 * public List<Map<String, String>> getChatHistory(Authentication auth) {
 *     String userId = auth.getName();  // 从认证会话获取，不信任前端传参
 *     List<ChatMessage> messages = chatMemoryConfig.getMessages(userId);
 *     ...
 * }
 * }</pre>
 *
 * @see <a href="https://owasp.org/www-project-top-ten/">OWASP Top 10 - A01:2021 Broken Access Control</a>
 */
@Controller
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
