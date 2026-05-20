package com.langchain4j.agentic._07_supervisor_orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.agentic.supervisor.AgentInvocation;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * 招聘主管接口 - 顶层招聘主管，负责协调候选人评估和决策制定
 */
public interface HiringSupervisor {
    /**
     * 调用招聘主管进行候选人评估和决策
     * @param request 用户请求（包含候选人信息和职位要求）
     * @param supervisorContext 主管上下文（提供行为指导策略）
     * @return 带有 AgenticScope 的结果，包含最终决策和执行过程信息
     */
    @Agent("顶层招聘主管，协调候选人评估和决策制定")
    ResultWithAgenticScope<String> invoke(@V("request") String request, @V("supervisorContext") String supervisorContext);
}
