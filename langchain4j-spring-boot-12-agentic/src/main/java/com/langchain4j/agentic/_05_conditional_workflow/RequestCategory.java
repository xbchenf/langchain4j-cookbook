package com.langchain4j.agentic._05_conditional_workflow;

/**
 * 用户请求分类枚举
 *
 * 定义企业智能问答系统支持的工单类型，
 * 作为条件工作流中路由判断的依据。
 */
public enum RequestCategory {
    /** 人事行政：招聘、考勤、福利、规章制度等 */
    HR,
    /** 技术支持：系统故障、软件使用、权限申请等 */
    TECHNICAL,
    /** 财务报销：费用报销、发票开具、预算审批等 */
    FINANCE,
    /** 无法归类：不属于以上任何类型的请求 */
    UNKNOWN
}
