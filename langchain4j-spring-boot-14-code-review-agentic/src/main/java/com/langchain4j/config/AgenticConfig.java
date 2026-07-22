package com.langchain4j.config;

import com.langchain4j.agent.*;
import com.langchain4j.nonai.StaticAnalyzer;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class AgenticConfig {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    // ============ Sequential Agents ============

    @Bean
    CodeParser codeParser() {
        return AgenticServices.agentBuilder(CodeParser.class)
                .chatModel(openAiChatModel)
                .outputKey("parsedCode")
                .build();
    }

    @Bean
    IssueIdentifier issueIdentifier() {
        return AgenticServices.agentBuilder(IssueIdentifier.class)
                .chatModel(openAiChatModel)
                .outputKey("codeIssues")
                .build();
    }

    // ============ Parallel Agents ============

    @Bean
    SecurityReviewer securityReviewer() {
        return AgenticServices.agentBuilder(SecurityReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("securityReview")
                .build();
    }

    @Bean
    PerformanceReviewer performanceReviewer() {
        return AgenticServices.agentBuilder(PerformanceReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("perfReview")
                .build();
    }

    @Bean
    MaintainabilityReviewer maintainabilityReviewer() {
        return AgenticServices.agentBuilder(MaintainabilityReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("maintReview")
                .build();
    }

    // ============ Loop Agents ============

    @Bean
    CodeFixer codeFixer() {
        return AgenticServices.agentBuilder(CodeFixer.class)
                .chatModel(openAiChatModel)
                .outputKey("fixedCode")
                .build();
    }

    @Bean
    ReReviewer reReviewer() {
        return AgenticServices.agentBuilder(ReReviewer.class)
                .chatModel(openAiChatModel)
                .outputKey("finalReview")
                .build();
    }

    // ============ Non-AI Agent ============

    @Bean
    StaticAnalyzer staticAnalyzer() {
        return new StaticAnalyzer();
    }
}
