package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CodeIssue {
    /** 问题类型: SECURITY / PERFORMANCE / MAINTAINABILITY / STYLE */
    String type;
    /** 严重程度: HIGH / MEDIUM / LOW */
    String severity;
    /** 问题所在行号 */
    Integer lineNumber;
    /** 问题所在文件 */
    String filePath;
    /** 问题标题，如 "SQL 注入风险" */
    String title;
    /** 问题描述 */
    String description;
    /** 修复建议 */
    String suggestion;
}
