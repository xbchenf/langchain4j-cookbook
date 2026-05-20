package com.langchain4j.agentic._03_loop_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 代码优化师 Agent
 * 负责根据审查反馈对代码进行优化改进，
 * 每次循环迭代时被调用，持续提升代码质量直到达到评分阈值。
 */
public interface CodeOptimizer {

    @UserMessage("""
        你是一位代码优化专家。请优化以下代码，提升代码质量。
        要求：保持原有功能逻辑不变，改进代码结构和可读性，完善异常处理和边界条件，优化命名风格。

        待优化代码：{{code}}
        """)
    @Agent("根据审查反馈优化代码，修复结构和规范问题")
    //@Agent(outputKey = "code", description = "根据审查反馈优化代码，修复结构和规范问题")
    String optimize(@V("code") String code);
}
