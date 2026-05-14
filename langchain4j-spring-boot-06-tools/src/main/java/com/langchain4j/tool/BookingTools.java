package com.langchain4j.tool;

import com.langchain4j.book.Booking;
import com.langchain4j.book.BookingService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 预订工具类
 *
 * 提供可供 AI 助手调用的预订相关工具方法。
 * LangChain4j 会通过 @Tool 注解将这些方法注册到 AI 模型，
 * 当用户询问预订相关问题时，AI 可以主动调用这些工具获取实时数据或执行操作。
 * 
 * 该类作为 Spring Component 被容器管理，并通过构造函数注入 BookingService。
 *
 * @author LangChain4j Cookbook
 * @version 1.0
 */
@Component
public class BookingTools {

    /** 预订服务，处理具体的业务逻辑 */
    private final BookingService bookingService;

    /**
     * 构造函数注入预订服务
     *
     * @param bookingService 预订服务实例，由 Spring 容器自动注入
     */
    public BookingTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 获取预订详情工具
     *
     * 当用户询问预订信息时，AI 助手会调用此方法获取指定预订的详细信息。
     *
     * @param bookingNumber   预订编号，如 "BK001"
     * @return 预订信息对象，包含日期、时间、状态等详细信息；如果未找到则返回 null
     */
    @Tool
    public Booking getBookingDetails(String bookingNumber) {
        return bookingService.getBookingDetails(bookingNumber);
    }

    /**
     * 取消预订工具
     *
     * 当用户请求取消预订时，AI 助手会调用此方法执行取消操作。
     * 需要提供预订编号和客户姓名进行身份验证，确保只有预订者本人才能取消。
     *
     * @param bookingNumber   预订编号，如 "BK001"
     * @param customerName    客户名字，如 "张三"
     */
    @Tool
    public void cancelBooking(String bookingNumber, String customerName) {
        bookingService.cancelBooking(bookingNumber, customerName);
    }
}