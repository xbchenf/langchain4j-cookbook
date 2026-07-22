package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Parallel-3: 可维护性审查
 * 专门审查命名、注释、代码结构、SOLID 原则等可维护性问题
 */
public interface MaintainabilityReviewer {

    @UserMessage("""
            请专门从【可维护性】维度审查以下 Java 代码。

            重点关注：
            1. 命名规范：类名/方法名/变量名是否符合 Java 命名惯例
            2. 注释质量：关键逻辑是否有注释、是否有过时注释
            3. 方法长度和复杂度：方法是否过长（>50行）、圈复杂度是否过高
            4. 单一职责：类和方法是否职责清晰
            5. 异常处理：是否吞掉异常、catch 块是否为空
            6. 代码重复：是否有可以抽取的重复代码

            对每个发现的问题给出评分（0-1）、详细描述和修复建议。
            最终给出整体可维护性评分（0.0-1.0）。

            已识别的问题列表（参考）：
            {{codeIssues}}

            原始代码：
            {{sourceCode}}
            """)
    @Agent("从可维护性维度审查代码：命名规范、注释、方法长度、单一职责、异常处理")
    String review(@V("sourceCode") String sourceCode,
                  @V("codeIssues") String codeIssues);
}
