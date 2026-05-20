package com.langchain4j.agentic._05_conditional_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 财务报销专家 Agent
 *
 * 专门处理财务相关咨询：费用报销流程、发票开具要求、差旅标准、
 * 预算审批、付款进度查询等问题。
 *
 * 仅在分类结果为 FINANCE 时被条件工作流激活。
 */
public interface FinanceExpert {

    @UserMessage("""
        你是一位资深财务专家，精通企业财务制度、税务法规和报销流程。
        请从财务专业角度回答以下用户问题。

        要求：
        - 回答准确严谨，引用公司财务制度
        - 涉及金额、税率等数字信息需格外准确
        - 说明审批流程和所需材料

        用户问题：{{request}}
        """)
    @Agent("从财务报销角度专业回答用户的问题")
    String answer(@V("request") String request);
}
