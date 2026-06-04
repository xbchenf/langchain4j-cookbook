package com.langchain4j.repository;

import com.langchain4j.entity.ChatHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistoryEntity, Long> {

    /**
     * 查询最近20条历史记录（按ID降序，最新的在前）
     * 用于前端展示
     */
    List<ChatHistoryEntity> findTop20BySessionIdOrderByIdDesc(String sessionId);

    /**
     * 查询最近20条历史记录（按ID升序，时间顺序）
     * 用于 AI 对话上下文（需要按时间顺序）
     */
    List<ChatHistoryEntity> findTop20BySessionIdOrderByIdAsc(String sessionId);


    void deleteBySessionId(String sessionId);
}
