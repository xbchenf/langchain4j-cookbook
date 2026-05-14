package com.langchain4j.book;

/**
 * 预订信息实体类
 *
 * 用于存储和传输预订相关的详细信息。
 * 该类作为数据传输对象（DTO），在 BookingService 和 BookingTools 之间传递预订数据。
 *
 * @author LangChain4j Cookbook
 * @version 1.0
 */
public class Booking {
    
    /** 预订编号，唯一标识一个预订 */
    private String bookingNumber;
    
    /** 客户名字 */
    private String customerName;
    
    /** 预订日期，格式：yyyy-MM-dd */
    private String date;
    
    /** 预订时间，格式：HH:mm */
    private String time;
    
    /** 预订状态：CONFIRMED（已确认）、PENDING（待处理）、CANCELLED（已取消） */
    private String status;
    
    /**
     * 默认构造函数
     * 
     * 用于反序列化或创建空对象时使用。
     */
    public Booking() {
    }
    
    /**
     * 带参数的构造函数
     * 
     * 用于快速创建一个完整的预订对象。
     *
     * @param bookingNumber   预订编号，如 "BK001"
     * @param customerName    客户名字，如 "John"
     * @param date            预订日期，格式：yyyy-MM-dd
     * @param time            预订时间，格式：HH:mm
     * @param status          预订状态，如 "CONFIRMED"、"PENDING"、"CANCELLED"
     */
    public Booking(String bookingNumber, String customerName,String date, String time, String status) {
        this.bookingNumber = bookingNumber;
        this.customerName = customerName;
        this.date = date;
        this.time = time;
        this.status = status;
    }
    
    /**
     * 获取预订编号
     *
     * @return 预订编号字符串
     */
    public String getBookingNumber() {
        return bookingNumber;
    }
    
    /**
     * 设置预订编号
     *
     * @param bookingNumber 预订编号字符串
     */
    public void setBookingNumber(String bookingNumber) {
        this.bookingNumber = bookingNumber;
    }
    
    /**
     * 获取客户名字
     *
     * @return 客户名字字符串
     */
    public String getCustomerName() {
        return customerName;
    }
    
    /**
     * 设置客户名字
     *
     * @param customerName 客户名字字符串
     */
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    /**
     * 获取预订日期
     *
     * @return 预订日期字符串，格式：yyyy-MM-dd
     */
    public String getDate() {
        return date;
    }
    
    /**
     * 设置预订日期
     *
     * @param date 预订日期字符串，格式：yyyy-MM-dd
     */
    public void setDate(String date) {
        this.date = date;
    }
    
    /**
     * 获取预订时间
     *
     * @return 预订时间字符串，格式：HH:mm
     */
    public String getTime() {
        return time;
    }
    
    /**
     * 设置预订时间
     *
     * @param time 预订时间字符串，格式：HH:mm
     */
    public void setTime(String time) {
        this.time = time;
    }
    
    /**
     * 获取预订状态
     *
     * @return 预订状态字符串，如 "CONFIRMED"、"PENDING"、"CANCELLED"
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * 设置预订状态
     *
     * @param status 预订状态字符串，如 "CONFIRMED"、"PENDING"、"CANCELLED"
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * 将预订对象转换为字符串表示形式
     * 
     * 用于调试和日志记录，展示预订的所有关键信息。
     *
     * @return 包含所有预订信息的字符串
     */
    @Override
    public String toString() {
        return "Booking{" +
                "bookingNumber='" + bookingNumber + '\'' +
                ", customerName='" + customerName + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}