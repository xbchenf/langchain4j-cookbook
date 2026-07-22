package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CombinedReviewResult {
    /** 安全性审查结果 */
    SecurityReview securityReview;
    /** 性能审查结果 */
    PerformanceReview performanceReview;
    /** 可维护性审查结果 */
    MaintainabilityReview maintainabilityReview;
    /** 综合质量评分：三维评分的平均值 */
    Double qualityScore;
    /** 综合风险等级: HIGH / MEDIUM / LOW */
    String riskLevel;
    /** 所有三维问题合并列表 */
    List<CodeIssue> allIssues;
}
