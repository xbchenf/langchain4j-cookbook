package com.langchain4j.tool;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 聊天模型监听器
 *
 * 用于监控和记录所有 ChatModel 的调用过程，包括：
 * - 请求发送前的日志记录
 * - 响应接收后的日志记录
 * - 错误发生时的异常信息记录
 *
 * 该监听器会被自动注入到应用上下文中的所有 ChatModel 和 StreamingChatModel，
 * 便于调试、性能分析和审计 AI 模型的调用情况。
 */
public class MyChatModelListener implements ChatModelListener {

    private static final Logger log = LoggerFactory.getLogger(MyChatModelListener.class);

    /**
     * 请求发送前回调
     *
     * 在向 AI 模型发送请求之前触发，记录请求详情（包括消息内容、模型参数等）。
     *
     * @param requestContext 请求上下文，包含完整的聊天请求信息
     */
    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        log.info("onRequest(): {}", requestContext.chatRequest());
    }

    /**
     * 响应接收后回调
     *
     * 在收到 AI 模型的响应后触发，记录响应详情（包括 AI 回答、token 使用量等）。
     *
     * @param responseContext 响应上下文，包含完整的聊天响应信息
     */
    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        log.info("onResponse(): {}", responseContext.chatResponse());
    }

    /**
     * 错误发生时的回调
     *
     * 当调用 AI 模型出现错误时触发，记录错误信息以便排查问题。
     *
     * @param errorContext 错误上下文，包含异常信息和相关上下文数据
     */
    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.info("onError(): {}", errorContext.error().getMessage());
    }
}
