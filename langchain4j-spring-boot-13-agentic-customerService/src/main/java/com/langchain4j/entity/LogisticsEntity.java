package com.langchain4j.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "logistics")
public class LogisticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 物流单号 */
    @Column(name = "tracking_no", nullable = false, unique = true, length = 50)
    private String trackingNo;

    /** 承运商 */
    @Column(name = "carrier", nullable = false, length = 30)
    private String carrier;

    /** 物流状态 */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 当前位置 */
    @Column(name = "current_location", length = 200)
    private String currentLocation;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
