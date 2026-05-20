package com.langchain4j.domain;

import dev.langchain4j.model.output.structured.Description;

/**
 * 简历结构化数据类 - 用于存储结构化的简历信息
 */
public class Cv {
    @Description("候选人的技能，以逗号分隔")
    private String skills;

    @Description("候选人的工作经验")
    private String professionalExperience;

    @Description("候选人的教育背景")
    private String studies;

    @Override
    public String toString() {
        return "简历:\n" +
                "技能 = \"" + skills + "\"\n" +
                "工作经验 = \"" + professionalExperience + "\"\n" +
                "教育背景 = \"" + studies + "\"\n";
    }
}
