package com.langchain4j.book;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 预订服务类
 *
 * 提供预订相关的业务逻辑处理，包括查询和取消预订等功能。
 * 该类被 Spring 容器管理，作为 Service 层组件供 BookingTools 调用。
 * 
 * 注意：当前实现使用内存中的 Map 模拟数据库，实际生产环境应替换为真实的数据库访问层。
 *
 * @author LangChain4j Cookbook
 * @version 1.0
 */
@Service
public class BookingService{
    
    /** 模拟数据库存储预订信息，Key 为预订编号，Value 为预订对象 */
    private final Map<String, Booking> bookingDatabase = new HashMap<>();
    
    /**
     * 构造函数，初始化示例数据
     * 
     * 在 Spring 容器创建 BookingService Bean 时，会自动调用此构造函数，
     * 并预加载一些示例预订数据用于演示和测试。
     */
    public BookingService() {
        // 添加示例预订数据用于演示，模拟数据库中的初始数据
        bookingDatabase.put("BK001", new Booking("BK001", "张三", "2026-05-15", "19:00", "CONFIRMED"));
        bookingDatabase.put("BK002", new Booking("BK002", "李四", "2026-05-16", "20:30", "CONFIRMED"));
        bookingDatabase.put("BK003", new Booking("BK003", "王五", "2026-05-17", "18:00", "PENDING"));
    }
    
    /**
     * 获取预订详情
     *
     * 根据预订编号和客户姓名查询预订信息。
     * 需要同时匹配预订编号、客户名字，确保数据安全性和隐私保护。
     *
     * @param bookingNumber   预订编号，如 "BK001"
     * @return 预订信息对象，如果未找到或信息不匹配则返回 null
     */
    public Booking getBookingDetails(String bookingNumber) {
        // 从内存数据库中查询预订信息（实际应用中应查询真实数据库）
        return bookingDatabase.get(bookingNumber);
    }
    
    /**
     * 取消预订
     *
     * 根据预订编号和客户姓名取消指定的预订。
     * 取消操作会将预订状态更新为 "CANCELLED"，但不会从数据库中删除记录。
     * 需要同时匹配预订编号、客户名字，确保只有预订者本人才能取消。
     *
     * @param bookingNumber   预订编号，如 "BK001"
     * @param customerName    客户名字，如 "张三"
     */
    public void cancelBooking(String bookingNumber, String customerName) {
        // 从内存数据库中查询预订信息（实际应用中应查询真实数据库）
        Booking booking = bookingDatabase.get(bookingNumber);
        
        // 验证客户姓名是否匹配，使用忽略大小写的比较方式提升用户体验
        if (booking != null && 
            booking.getCustomerName().equalsIgnoreCase(customerName) ) {
            
            // 更新预订状态为已取消，保留历史记录便于后续查询和统计
            booking.setStatus("CANCELLED");
        }
        // 如果预订不存在或客户信息不匹配，则不执行任何操作（静默失败）
    }
}