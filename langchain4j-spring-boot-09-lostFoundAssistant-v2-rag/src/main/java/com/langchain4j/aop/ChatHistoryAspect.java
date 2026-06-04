package com.langchain4j.aop;

import com.langchain4j.entity.ChatHistoryEntity;
import com.langchain4j.repository.ChatHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 聊天历史切面
 * 拦截标注了 @ChatHistoryAop 的方法，自动保存用户和 AI 的聊天消息到数据库
 */
@Component
@Aspect
@Slf4j
public class ChatHistoryAspect {

    //用户消息
    private String userRole="0";
    //AI消息
    private String aiRole="1";

    @Around("@annotation(com.langchain4j.aop.ChatHistoryAop)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String sessionId = args[0].toString();
        String message = args[1].toString();
        log.info("sessionId:{}，用户消息:{}",sessionId,message);
        saveHistoryMessage(sessionId,message,userRole);
        Object result = joinPoint.proceed();
        saveHistoryMessage(sessionId,result.toString(),aiRole);
        return result;
    }

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;
    public void saveHistoryMessage(String sessionId,String message,String role){
        ChatHistoryEntity chatHistoryEntity=new ChatHistoryEntity();
        chatHistoryEntity.setSessionId(sessionId);
        chatHistoryEntity.setRole(role);
        chatHistoryEntity.setContent(message);
        chatHistoryRepository.save(chatHistoryEntity);
    }
}
