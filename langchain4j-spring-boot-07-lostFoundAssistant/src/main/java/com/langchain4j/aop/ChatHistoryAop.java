package com.langchain4j.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 聊天历史注解
 * 标注在需要保存聊天记录的方法上，由 ChatHistoryAspect 自动拦截并保存
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatHistoryAop {
}
