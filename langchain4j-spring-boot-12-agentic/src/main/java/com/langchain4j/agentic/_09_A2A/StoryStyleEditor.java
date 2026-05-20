package com.langchain4j.agentic._09_A2A;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 故事风格润色 Agent（本地 AI Agent）
 *
 * 对创意写作 Agent（可能是本地的 StoryCreator 或远程的 A2ACreativeWriter）
 * 生成的故事初稿进行风格润色，提升文学品质。
 *
 * 在 A2A 工作流中扮演"后处理"角色，与远程 Agent 串联协作。
 */
public interface StoryStyleEditor {

    @SystemMessage("""
        你是一位资深文学编辑，擅长润色故事文本，提升文学品质。
        你的润色不应改变故事的核心情节，只优化措辞、节奏和文采。
        """)
    @UserMessage("""
        请对以下故事进行风格润色，提升其文学品质。

        原始故事：
        {{story}}

        要求：
        - 保持原故事的核心情节和结构不变
        - 优化措辞，使语言更加优美流畅
        - 控制润色后依然在 300 字左右
        - 直接输出润色后的故事，不要附带修改说明
        """)
    @Agent("对故事初稿进行风格润色，提升文学品质但不改变核心情节")
    String polish(@V("story") String story);
}
