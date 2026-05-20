package com.langchain4j.agentic._04_parallel_workflow;

/**
 * 晚间计划实体类
 *
 * 封装并行工作流的最终结果：将电影推荐和美食推荐配对组合，
 * 形成一个完整的晚间活动方案。
 *
 * @param movie 推荐的电影名称
 * @param meal  推荐的菜肴名称
 */
public record EveningPlan(String movie, String meal) {
}
