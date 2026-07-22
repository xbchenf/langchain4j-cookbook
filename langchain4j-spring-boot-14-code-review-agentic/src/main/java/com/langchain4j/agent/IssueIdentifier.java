package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sequential-2: 基于代码解析结果，识别潜在问题
 * 接收 CodeParser 的输出，生成问题清单
 */
public interface IssueIdentifier {

    @UserMessage("""
            基于以下代码解析结果，识别代码中的所有潜在问题。

            请关注以下类别的问题：
            1. 安全性：SQL 注入、XSS、敏感信息泄露、缺少输入校验
            2. 性能：N+1 查询、不必要的循环、阻塞调用、资源未释放
            3. 可维护性：命名不规范、缺少注释、方法过长、圈复杂度高、异常被吞掉
            4. Bug 风险：空指针风险、类型转换错误、逻辑错误

            对于每个问题，请给出：
            - 问题类型（SECURITY / PERFORMANCE / MAINTAINABILITY / STYLE）
            - 严重程度（HIGH / MEDIUM / LOW）
            - 所在行号
            - 问题标题
            - 详细描述
            - 修复建议

            代码解析结果：
            {{parsedCode}}

            原始代码：
            {{sourceCode}}
            """)
    @Agent("基于代码结构解析结果，识别安全性、性能、可维护性方面的问题")
    String identify(@V("parsedCode") String parsedCode,
                    @V("sourceCode") String sourceCode);
}
