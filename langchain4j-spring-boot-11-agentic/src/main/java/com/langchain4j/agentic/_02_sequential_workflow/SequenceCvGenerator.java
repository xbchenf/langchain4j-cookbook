package com.langchain4j.agentic._02_sequential_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.V;

import java.util.Map;

/**
 * 序列化简历生成器接口 - 定义组合多个Agent的顺序工作流
 * 该接口将简历生成和简历定制两个步骤组合成一个完整的工作流
 */
public interface SequenceCvGenerator {
    /**
     * 根据用户提供的信息生成简历，并根据指令进行定制
     * @param lifeStory 用户的生活故事和职业经历
     * @param instructions 定制指令（如职位描述）
     * @return 包含结果和代理作用域的包装对象，结果为包含lifeStory、masterCv和tailoredCv的Map
     */
    @Agent("基于用户信息生成简历并根据指令定制，保持简洁，避免空行")
    ResultWithAgenticScope<Map<String, String>> generateTailoredCv(@V("lifeStory") String lifeStory, @V("instructions") String instructions);
}
