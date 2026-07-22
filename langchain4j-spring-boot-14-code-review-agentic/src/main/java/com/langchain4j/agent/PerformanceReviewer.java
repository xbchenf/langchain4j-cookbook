package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Parallel-2: 性能审查
 * 专门审查 N+1 查询、阻塞调用、资源泄露等性能问题
 */
public interface PerformanceReviewer {

    @UserMessage("""
            请专门从【性能】维度审查以下 Java 代码。

            重点关注：
            1. N+1 查询：循环内执行数据库查询
            2. 阻塞调用：Thread.sleep、同步等待 I/O
            3. 资源泄露：未关闭的连接/流/文件句柄
            4. 不必要的对象创建：循环内 new 大对象
            5. 缓存缺失：重复计算或查询

            对每个发现的问题给出评分（0-1）、详细描述和修复建议。
            最终给出整体性能评分（0.0-1.0）。

            已识别的问题列表（参考）：
            {{codeIssues}}

            原始代码：
            {{sourceCode}}
            """)
    @Agent("从性能维度审查代码：N+1查询、阻塞调用、资源泄露、对象创建、缓存")
    String review(@V("sourceCode") String sourceCode,
                  @V("codeIssues") String codeIssues);
}
