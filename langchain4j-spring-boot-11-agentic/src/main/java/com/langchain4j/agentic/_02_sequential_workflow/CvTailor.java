package com.langchain4j.agentic._02_sequential_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 简历定制器接口 - 根据特定职位描述或要求定制简历
 */
public interface CvTailor {

    /**
     * 根据特定指令定制简历，使其更符合目标职位的要求
     * @param masterCv 原始完整简历
     * @param instructions 定制指令（如职位描述、具体要求等）
     * @return 定制后的简历
     */
    @Agent("根据特定指令定制简历")
    @SystemMessage("""
            以下是一份需要根据特定职位描述、反馈或其他指令进行定制的简历。
            你可以优化简历以满足要求，但不要虚构事实。
            如果删除不相关的内容能使简历更好地符合指令，可以删除。
            目标是让申请人获得面试机会，并能够在面试中展现简历中的能力。不要让简历过长。
            原始完整简历：{{masterCv}}
            """)
    @UserMessage("""
            以下是定制简历的指令：{{instructions}}
            """)
    String tailorCv(@V("masterCv") String masterCv, @V("instructions") String instructions);
}
