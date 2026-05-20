package com.langchain4j.a2a;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * A2A AgentCard 配置
 *
 * 定义"创意写作助手"Agent 的元数据，包括名称、能力、技能等。
 * A2A 客户端通过 GET /.well-known/agent-card.json 获取此信息，
 * 从而自动发现 Agent 的能力和通信方式。
 */
@Configuration
public class StoryAgentCardProducer {

    @Value("${server.port}")
    private int port;

    @Bean
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name("创意写作助手")
                .description("根据主题创作富有想象力的短篇故事。支持中文和英文创作，故事结构完整，语言生动。")
                .url("http://localhost:" + port)
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        new AgentSkill.Builder()
                                .id("creative_writing")
                                .name("创意写作")
                                .description("根据给定主题创作富有想象力的短篇故事，约300字")
                                .tags(List.of("写作", "创意", "故事"))
                                .examples(List.of(
                                        "写一个关于龙与魔法师的故事",
                                        "创作一个关于太空探险的短篇故事",
                                        "写一个关于友情的故事"
                                ))
                                .build()
                ))
                .build();
    }
}
