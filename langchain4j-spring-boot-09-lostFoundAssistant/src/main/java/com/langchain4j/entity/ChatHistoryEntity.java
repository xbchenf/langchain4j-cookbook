package com.langchain4j.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "chat_history")
@Comment("聊天历史")
public class ChatHistoryEntity {

    /** 记录ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("会话ID")
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @Comment("角色")
    @Column(name = "role", nullable = false, length = 100)
    private String role;

    @Comment("内容")
    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

}
