package com.langchain4j.agentic._03_loop_workflow;

import com.langchain4j.domain.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 带评分的简历定制器接口 - 根据评审反馈定制简历
 */
public interface ScoredCvTailor {

    /**
     * 根据特定指令和评审反馈定制简历
     * @param cv 当前简历
     * @param cvReview 包含评分和反馈的评审结果
     * @return 定制后的简历
     */
    @Agent("根据特定指令定制简历")
    @SystemMessage("""
            以下是一份需要根据特定职位描述、反馈或其他指令进行定制的简历。
            你可以优化简历以满足要求，但不要虚构事实。
            如果删除不相关的内容能使简历更好地符合指令，可以删除。
            目标是让申请人获得面试机会，并能够在面试中展现简历中的能力。
            当前简历：{{cv}}
            """)
    @UserMessage("""
            以下是定制简历的指令和反馈：
            （再次强调，不要发明原始简历中不存在的事实。
            如果申请人不适合，突出其现有特征中最匹配的部分，但不要编造事实）
            评审结果：{{cvReview}}
            """)
    String tailorCv(@V("cv") String cv, @V("cvReview") CvReview cvReview);
}
