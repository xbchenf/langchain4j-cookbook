package com.langchain4j.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订单号 */
    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;

    /** 客户姓名 */
    @Column(name = "customer_name", nullable = false, length = 50)
    private String customerName;

    /** 客户电话 */
    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    /** 商品名称 */
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    /** 商品价格 */
    @Column(name = "product_price", precision = 10, scale = 2)
    private BigDecimal productPrice;

    /** 订单状态 */
    @Column(name = "order_status", nullable = false, length = 20)
    private String orderStatus;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
