package com.langchain4j.agentic._08_HumanInTheLoop;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 星座运势生成 Agent
 *
 * 根据用户的姓名和星座，生成个性化的星座运势。
 * 需要两个信息：姓名（用户通常主动提供）和星座（用户可能忘记提供）。
 *
 * 当缺少星座信息时，由 HumanInTheLoop 机制向用户追问，
 * 体现出人机协作的工作流模式。
 */
public interface AstrologyAgent {

    @SystemMessage("""
        你是一位经验丰富的星座占星师，擅长根据星座和姓名生成富有洞察力的运势分析。
        运势内容应涵盖爱情、事业、健康三个方面。
        语气温暖鼓励，带有中式占星风格。
        """)
    @UserMessage("""
        请为 {{name}} 生成今日星座运势。他/她的星座是 {{sign}}。
        运势应包含爱情、事业、健康三个方面的分析，控制在 200 字以内。
        """)
    @Agent("根据用户的姓名和星座生成今日运势分析")
    String horoscope(@V("name") String name, @V("sign") String sign);
}
