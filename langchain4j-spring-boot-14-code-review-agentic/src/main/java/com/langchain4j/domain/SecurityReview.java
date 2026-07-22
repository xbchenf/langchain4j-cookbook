package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SecurityReview {
    /** 安全评分 0.0-1.0 */
    Double score;
    /** 发现的安全问题列表 */
    List<CodeIssue> issues;
    /** 总体评价 */
    String summary;
}
