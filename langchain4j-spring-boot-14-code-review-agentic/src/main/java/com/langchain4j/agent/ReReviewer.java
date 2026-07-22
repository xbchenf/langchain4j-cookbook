package com.langchain4j.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Loop-2: 对修复后的代码重新审查，给出质量评分
 */
public interface ReReviewer {

    @UserMessage("""
            请重新审查修复后的代码，重点检查：
            1. 之前发现的问题是否已修复
            2. 修复是否引入了新的问题
            3. 代码整体质量是否有提升

            给出最终质量评分（0.0-1.0）和简要评价。
            只输出评分和评价，格式为：
            评分: 0.XX
            评价: ...

            原始问题列表：
            {{combinedReview}}

            修复后的代码：
            {{fixedCode}}
            """)
    @Agent("重新审查修复后的代码，验证问题是否已解决，给出质量评分")
    String review(@V("combinedReview") String combinedReview,
                  @V("fixedCode") String fixedCode);
}
