package com.langchain4j.agentic._04_parallel_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * 晚间活动策划 Agent（并行工作流编排器）
 *
 * 并行调用美食专家和电影专家两个子 Agent，
 * 将各自独立返回的结果组合成一套完整的晚间活动方案。
 *
 * 并行工作流适用于以下场景：
 * - 多个子任务之间没有依赖关系，可以并发执行
 * - 需要同时从多个数据源获取信息后汇总
 * - 需要缩短整体响应时间，以最慢的子任务为准
 */
public interface EveningPlannerAgent {

    @UserMessage("""
        你是一位贴心的晚间活动策划师。
        用户当前心情是「{{mood}}」，希望度过一个愉快的夜晚。

        请同时调用以下两个工具获取推荐，它们可以并行执行：
        - "根据用户心情推荐 3 道合适的菜肴"
        - "根据用户心情推荐 3 部合适的电影"
        """)
    @Agent("并行调用美食和电影推荐工具，获取晚间活动所需的推荐结果")
    List<EveningPlan> plan(@V("mood") String mood);
}
