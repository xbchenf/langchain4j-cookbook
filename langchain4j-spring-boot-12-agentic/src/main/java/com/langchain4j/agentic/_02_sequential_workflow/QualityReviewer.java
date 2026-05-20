package com.langchain4j.agentic._02_sequential_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 质量审核师 Agent
 * 负责对技术方案进行终审，确保方案的完整性、可行性和专业性，
 * 作为顺序工作流的最后一步，产出可交付的最终技术方案。
 */
public interface QualityReviewer {

    @UserMessage("""
        你是一位技术方案审核专家。请审核以下技术方案并输出优化后的最终版本。

        审核要点：
        1. 方案是否完整覆盖需求，有无遗漏
        2. 技术选型是否合理，有无明显风险
        3. 表达是否清晰专业，需要优化的地方直接修改

        待审核方案：{{document}}
        """)
    @Agent("审核技术方案的完整性和可行性，输出最终方案")
    String review(@V("document") String document);
}
