package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PerformanceReview {
    /** 性能评分 0.0-1.0 */
    Double score;
    /** 发现的性能问题列表 */
    List<CodeIssue> issues;
    /** 总体评价 */
    String summary;
}
