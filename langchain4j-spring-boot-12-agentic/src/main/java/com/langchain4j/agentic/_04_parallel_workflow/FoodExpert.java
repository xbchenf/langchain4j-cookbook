package com.langchain4j.agentic._04_parallel_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * 美食推荐 Agent
 *
 * 根据给定的心情/氛围，推荐 3 道适合的菜肴。
 * 作为并行工作流的一个子 Agent 独立运行，与电影推荐 Agent 并发执行。
 */
public interface FoodExpert {

    @UserMessage("""
        你是一位资深美食推荐专家，精通各国料理。
        请根据用户当前的心情，推荐 3 道与之匹配的菜肴。

        用户心情：{{mood}}

        要求：
        - 每道菜只给出菜名即可，无需解释
        - 菜名应当简洁且对中文用户具有吸引力
        - 严格只返回包含 3 道菜的列表，不要包含任何其他内容
        """)
    @Agent("根据用户心情推荐 3 道合适的菜肴")
    List<String> findMeal(@V("mood") String mood);
}
