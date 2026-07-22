package com.langchain4j.nonai;

import com.langchain4j.domain.CodeIssue;
import com.langchain4j.domain.CodeIssueType;
import com.langchain4j.domain.Severity;
import com.langchain4j.domain.StaticAnalysisResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Non-AI Agent —— 纯 Java 规则引擎做静态分析
 *
 * 负责确定性检查（不需要 LLM）：
 * - 圈复杂度估算
 * - 方法行数检查
 * - 命名规范检查
 *
 * 作为一等公民参与 Agentic 工作流，和 AI Agent 互操作。
 */
public class StaticAnalyzer {

    @Agent(description = "使用规则引擎对源代码进行静态分析（圈复杂度、行数、命名规范）",
           outputKey = "staticResult")
    public StaticAnalysisResult analyze(@V("sourceCode") String sourceCode) {

        System.out.println("[Non-AI Agent] StaticAnalyzer 开始分析...");

        List<CodeIssue> issues = new ArrayList<>();

        // 1. 估算圈复杂度（基于 if/for/while/case/catch 分支数）
        int complexity = countPattern(sourceCode, "\\b(if|for|while|case|catch)\\b");
        boolean complexityWarning = complexity > 10;

        if (complexityWarning) {
            issues.add(CodeIssue.builder()
                    .type(CodeIssueType.STYLE)
                    .severity(Severity.MEDIUM)
                    .title("圈复杂度过高: " + complexity)
                    .description("方法中检测到 " + complexity + " 个分支点，圈复杂度超过建议值 10")
                    .suggestion("考虑拆分复杂方法、提取子方法、使用策略模式")
                    .build());
        }

        // 2. 检查方法行数
        String[] lines = sourceCode.split("\n");
        int maxMethodLines = findLongestMethod(lines);
        boolean methodLengthWarning = maxMethodLines > 50;

        if (methodLengthWarning) {
            issues.add(CodeIssue.builder()
                    .type(CodeIssueType.STYLE)
                    .severity(Severity.LOW)
                    .title("方法过长: " + maxMethodLines + " 行")
                    .description("检测到方法超过 50 行（最长方法 " + maxMethodLines + " 行）")
                    .suggestion("考虑将长方法拆分为多个职责单一的小方法")
                    .build());
        }

        // 3. 检查命名规范
        List<String> namingIssues = checkNaming(lines);

        for (String nameIssue : namingIssues) {
            issues.add(CodeIssue.builder()
                    .type(CodeIssueType.STYLE)
                    .severity(Severity.LOW)
                    .title("命名不规范: " + nameIssue)
                    .description("变量/方法名不符合 Java 命名规范")
                    .suggestion("使用 camelCase 命名变量和方法，PascalCase 命名类")
                    .build());
        }

        System.out.println("[Non-AI Agent] StaticAnalyzer 完成：圈复杂度=" + complexity +
                "，最长方法=" + maxMethodLines + "行，命名问题=" + namingIssues.size());

        return StaticAnalysisResult.builder()
                .cyclomaticComplexity(complexity)
                .complexityWarning(complexityWarning)
                .maxMethodLines(maxMethodLines)
                .methodLengthWarning(methodLengthWarning)
                .namingIssues(namingIssues)
                .issues(issues)
                .build();
    }

    /** 统计正则匹配次数 */
    private int countPattern(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /** 找出最长的代码块（方法） */
    private int findLongestMethod(String[] lines) {
        int maxLen = 0, currentLen = 0;
        boolean inMethod = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches(".*(public|private|protected)\\s+.*\\{.*")) {
                inMethod = true;
                currentLen = 1;
            } else if (inMethod) {
                currentLen++;
                if (trimmed.equals("}") || trimmed.startsWith("}")) {
                    maxLen = Math.max(maxLen, currentLen);
                    inMethod = false;
                }
            }
        }
        return maxLen;
    }

    /** 检查不符合 Java 命名规范的标识符 */
    private List<String> checkNaming(String[] lines) {
        List<String> issues = new ArrayList<>();
        Pattern snakeCasePattern = Pattern.compile("\\b([a-z]+_[a-z]+)\\b");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("String ") || line.contains("int ") ||
                line.contains("boolean ") || line.contains("double ")) {
                Matcher m = snakeCasePattern.matcher(line);
                while (m.find()) {
                    String name = m.group(1);
                    if (name.contains("_") && !name.startsWith("_")) {
                        issues.add("第" + (i + 1) + "行: " + name + " (应使用 camelCase)");
                    }
                }
            }
        }
        return issues;
    }
}
