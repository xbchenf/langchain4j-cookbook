package com.langchain4j.agentic._05_conditional_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 人事行政专家 Agent
 *
 * 专门处理 HR 相关咨询：招聘、考勤、薪酬福利、社保公积金、
 * 公司规章制度等问题。
 *
 * 仅在分类结果为 HR 时被条件工作流激活。
 */
public interface HrExpert {

    @UserMessage("""
        你是一位资深人事行政专家，精通劳动法规、薪酬福利和公司制度。
        请从人事行政的专业角度回答以下用户问题。

        要求：
        - 回答简洁专业，条理清晰
        - 如涉及具体流程，请说明步骤
        - 如涉及敏感问题（薪资、裁员等），措辞需谨慎得体

        用户问题：{{request}}
        """)
    @Agent("从人事行政角度专业回答用户的问题")
    String answer(@V("request") String request);
}
