package com.langchain4j.agentic._05_conditional_workflow;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 组织工具类 - 提供邮件发送、日历安排等组织功能的工具方法
 */
public class OrganizingTools {

    /**
     * 获取当前日期
     * @return 当前日期
     */
    @Tool
    public Date getCurrentDate(){
        return new Date();
    }

    /**
     * 查找给定职位描述 ID 需要参加现场面试的人员的邮箱地址和姓名
     * @param jobDescriptionId 职位描述 ID
     * @return 相关人员列表（格式：姓名: 邮箱）
     */
    @Tool("查找给定职位描述 ID 需要参加现场面试的人员的邮箱地址和姓名")
    public List<String> getInvolvedEmployeesForInterview(@P("职位描述 ID") String jobDescriptionId){
        System.out.println("***职位描述 ID ："+jobDescriptionId);
        // 演示用的虚拟实现
        return new ArrayList<>(List.of(
                "Anna Bolena: hiring.manager@company.com",
                "Chris Durue: near.colleague@company.com",
                "Esther Finnigan: vp@company.com"));
    }

    /**
     * 根据邮箱地址为员工创建日历条目
     * @param emailAddress 员工邮箱地址列表
     * @param topic 会议主题
     * @param start 开始日期和时间，格式为 yyyy-mm-dd hh:mm
     * @param end 结束日期和时间，格式为 yyyy-mm-dd hh:mm
     */
    @Tool("根据邮箱地址为员工创建日历条目")
    public void createCalendarEntry(@P("员工邮箱地址列表") List<String> emailAddress, @P("会议主题") String topic, @P("开始日期和时间，格式为 yyyy-mm-dd hh:mm") String start, @P("结束日期和时间，格式为 yyyy-mm-dd hh:mm") String end){
        // 演示用的虚拟实现
        System.out.println("*** 已创建日历条目 ***");
        System.out.println("主题：" + topic);
        System.out.println("开始时间：" + start);
        System.out.println("结束时间：" + end);
    }

    /**
     * 发送邮件
     * @param to 收件人邮箱地址列表
     * @param cc 抄送人邮箱地址列表
     * @param subject 邮件主题
     * @param body 邮件正文
     * @return 邮件 ID
     */
    @Tool
    public int sendEmail(@P("收件人邮箱地址列表") List<String> to, @P("抄送人邮箱地址列表") List<String> cc, @P("邮件主题") String subject, @P("邮件正文") String body){
        // 演示用的虚拟实现
        System.out.println("*** 已发送邮件 ***");
        System.out.println("收件人：" + to);
        System.out.println("抄送人：" + cc);
        System.out.println("主题：" + subject);
        System.out.println("正文：" + body);
        return 1234; // 虚拟邮件 ID
    }

    /**
     * 更新申请状态
     * @param jobDescriptionId 职位描述 ID
     * @param candidateName 候选人姓名（名，姓）
     * @param newStatus 新的申请状态
     */
    @Tool
    public void updateApplicationStatus(@P("职位描述 ID") String jobDescriptionId, @P("候选人姓名（名，姓）") String candidateName, @P("新的申请状态") String newStatus){
        // 演示用的虚拟实现
        System.out.println("*** 申请状态已更新 ***");
        System.out.println("职位描述 ID：" + jobDescriptionId);
        System.out.println("候选人姓名：" + candidateName);
        System.out.println("新状态：" + newStatus);
    }
}
