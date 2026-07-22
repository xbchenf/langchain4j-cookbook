package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MaintainabilityReview {
    /** 可维护性评分 0.0-1.0 */
    Double score;
    /** 发现的可维护性问题列表 */
    List<CodeIssue> issues;
    /** 总体评价 */
    String summary;
}
