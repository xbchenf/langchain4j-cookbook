package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sequential-1: 解析 Java 源代码结构
 * 提取类名、方法签名、字段列表、依赖注入点等结构化信息
 */
public interface CodeParser {

    @UserMessage("""
            请解析以下 Java 源代码，提取其结构化信息。

            返回格式：
            1. 类名和包路径
            2. 方法列表（方法名、参数、返回类型、行号范围）
            3. 字段列表（字段名、类型、注解）
            4. 依赖注入点（@Autowired / 构造函数注入）
            5. 异常处理块（try-catch 位置）

            只输出结构化解析结果，不要评价代码质量。

            源代码：
            {{sourceCode}}
            """)
    @Agent("解析 Java 源代码的结构化信息（类/方法/字段/依赖）")
    String parse(@V("sourceCode") String sourceCode);
}
