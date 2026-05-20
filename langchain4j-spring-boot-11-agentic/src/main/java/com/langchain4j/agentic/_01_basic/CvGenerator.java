package com.langchain4j.agentic._01_basic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 简历生成器接口 - 定义根据用户提供的个人信息生成简历的AI代理行为
 */
public interface CvGenerator {
    /**
     * 根据用户的生活故事和职业经历生成完整的简历
     * @param userInfo 用户的生活故事和职业经历信息
     * @return 生成的完整简历文本
     */
    @UserMessage("""
            以下是我的人生和职业发展轨迹信息，
            请将其整理成一份清晰完整的简历。
            不要虚构事实，也不要遗漏任何技能或经历。
            这份简历稍后会被进一步清理，目前请确保内容完整。
            只返回简历内容，不要有其他文字。
            我的个人经历：{{lifeStory}}
            """)
    @Agent("基于用户提供的信息生成清晰的简历")
    String generateCv(@V("lifeStory") String userInfo);
}
