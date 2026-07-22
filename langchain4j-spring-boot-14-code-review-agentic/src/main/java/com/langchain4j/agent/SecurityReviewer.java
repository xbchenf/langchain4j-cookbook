package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Parallel-1: 安全性审查
 * 专门审查 SQL 注入、XSS、敏感信息泄露等安全问题
 */
public interface SecurityReviewer {

    @UserMessage("""
            请专门从【安全性】维度审查以下 Java 代码。

            重点关注：
            1. SQL 注入：字符串拼接构建 SQL、未使用参数化查询
            2. XSS 攻击：未转义的用户输入输出到页面
            3. 敏感信息泄露：异常堆栈返回前端、日志打印敏感数据
            4. 输入校验：对外部输入缺少校验和过滤
            5. 权限控制：缺少访问控制注解或检查

            对每个发现的问题给出评分（0-1）、详细描述和修复建议。
            最终给出整体安全评分（0.0-1.0）。

            已识别的问题列表（参考）：
            {{codeIssues}}

            原始代码：
            {{sourceCode}}
            """)
    @Agent("从安全性维度审查代码：SQL注入、XSS、敏感信息泄露、输入校验、权限控制")
    String review(@V("sourceCode") String sourceCode,
                  @V("codeIssues") String codeIssues);
}
