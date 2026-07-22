package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Loop-1: 根据审查意见生成修复后的代码
 */
public interface CodeFixer {

    @UserMessage("""
            请根据以下审查意见，修复原始代码中的问题。

            修复要求：
            1. 只修复审查意见中指出 MEDIUM 和 HIGH 严重度的问题
            2. 保持代码原有结构和逻辑不变
            3. 修复后代码必须语法正确
            4. 只输出修复后的完整代码，不要添加任何解释

            审查意见：
            {{combinedReview}}

            原始代码：
            {{sourceCode}}
            """)
    @Agent("根据审查意见修复代码中的问题，输出修复后的完整代码")
    String fix(@V("combinedReview") String combinedReview,
               @V("sourceCode") String sourceCode);
}
