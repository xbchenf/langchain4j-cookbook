package com.langchain4j.agentic._05_conditional_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 工单分类路由 Agent
 *
 * 分析用户请求内容，判断其所属的业务类型。
 * 作为条件工作流的第一步，输出分类结果供后续路由决策使用。
 *
 * 职责单一：只做分类，不回答问题。
 */
public interface CategoryRouter {

    @UserMessage("""
        你是一位经验丰富的企业客服主管，擅长快速判断用户咨询的业务类型。
        请分析以下用户请求，判断它属于哪个类别。

        分类标准：
        - HR（人事行政）：招聘求职、考勤请假、薪酬福利、社保公积金、公司规章制度等
        - TECHNICAL（技术支持）：IT系统故障、软件使用问题、账号权限、网络连接等
        - FINANCE（财务报销）：费用报销流程、发票开具、差旅标准、预算审批、付款进度等
        - UNKNOWN（其他）：不属于以上任何类别的请求

        用户请求：{{request}}

        请只回复一个类别名称（HR / TECHNICAL / FINANCE / UNKNOWN），不要包含任何其他内容。
        """)
    @Agent("分析用户请求内容，判断其属于人事、技术、财务还是其他类别")
    RequestCategory classify(@V("request") String request);
}
