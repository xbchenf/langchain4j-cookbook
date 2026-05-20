package com.langchain4j.agentic._03_loop_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 代码审查员 Agent
 * 负责对代码进行质量评分（0.0-1.0），
 * 评分结果用于循环工作流的退出条件判断。
 */
public interface CodeReviewer {

    @UserMessage("""
        你是一位严格的代码审查员。请对以下代码从四个维度评分，给出 0.0-1.0 的综合分数。
        评分维度：代码结构清晰度（30%）、命名规范和可读性（30%）、异常处理完整性（25%）、最佳实践（15%）。
        只返回一个数字，不要返回其他内容。

        代码：{{code}}
        """)
    @Agent("对代码进行多维质量评分，返回 0.0-1.0 之间的分数")
    //@Agent(outputKey = "score", description = "对代码进行多维质量评分，返回 0.0-1.0 之间的分数")
    double reviewScore(@V("code") String code);
}
