package com.langchain4j.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "returns_table")
public class ReturnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 退货单号 */
    @Column(name = "return_no", nullable = false, unique = true, length = 30)
    private String returnNo;

    /** 关联订单ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 退货原因 */
    @Column(name = "reason", length = 500)
    private String reason;

    /** 状态：已提交/审核中/已退款/已拒绝 */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 退货运单号 */
    @Column(name = "logistics_no", length = 50)
    private String logisticsNo;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
