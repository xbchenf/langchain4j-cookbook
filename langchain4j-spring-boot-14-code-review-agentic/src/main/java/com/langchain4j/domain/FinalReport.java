package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FinalReport {
    /** 审查的源文件名 */
    String sourceFileName;
    /** 静态分析结果 */
    StaticAnalysisResult staticAnalysis;
    /** AI 审查结果 */
    CombinedReviewResult aiReview;
    /** 修复结果（如有自动修复） */
    FixResult fixResult;
    /** 最终质量评分 0.0-1.0 */
    Double finalScore;
    /** AI 生成的审查总结 */
    String summary;
}
