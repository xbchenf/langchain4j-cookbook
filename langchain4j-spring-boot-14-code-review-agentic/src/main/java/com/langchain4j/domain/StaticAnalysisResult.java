package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StaticAnalysisResult {
    /** 圈复杂度 */
    Integer cyclomaticComplexity;
    /** 是否超过圈复杂度阈值 (>10 警告) */
    Boolean complexityWarning;
    /** 方法最大行数 */
    Integer maxMethodLines;
    /** 是否超过方法行数阈值 (>50 警告) */
    Boolean methodLengthWarning;
    /** 命名不规范的方法/字段列表 */
    List<String> namingIssues;
    /** 静态分析发现的所有问题 */
    List<CodeIssue> issues;
}
