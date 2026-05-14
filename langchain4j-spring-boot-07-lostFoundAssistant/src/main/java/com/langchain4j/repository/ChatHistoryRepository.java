package com.langchain4j.repository;

import com.langchain4j.entity.ChatHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistoryEntity, Long> {

    List<ChatHistoryEntity> findTop20BySessionIdOrderByIdDesc(String sessionId);


    void deleteBySessionId(String sessionId);
}
