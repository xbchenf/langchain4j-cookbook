package com.langchain4j.agentic._04_parallel_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * 电影推荐 Agent
 *
 * 根据给定的心情/氛围，推荐 3 部适合的电影。
 * 作为并行工作流的一个子 Agent 独立运行，与美食推荐 Agent 并发执行。
 */
public interface MovieExpert {

    @UserMessage("""
        你是一位资深电影推荐专家，对全球电影如数家珍。
        请根据用户当前的心情，推荐 3 部与之匹配的电影。

        用户心情：{{mood}}

        要求：
        - 每部电影只给出片名即可，无需解释
        - 片名应当简洁且对中文用户具有吸引力
        - 严格只返回包含 3 部电影的列表，不要包含任何其他内容
        """)
    @Agent("根据用户心情推荐 3 部合适的电影")
    List<String> findMovie(@V("mood") String mood);
}
