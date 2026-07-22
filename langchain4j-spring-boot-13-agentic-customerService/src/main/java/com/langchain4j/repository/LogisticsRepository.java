package com.langchain4j.repository;

import com.langchain4j.entity.LogisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogisticsRepository extends JpaRepository<LogisticsEntity, Long> {

    /** 根据物流单号查询 */
    LogisticsEntity findByTrackingNo(String trackingNo);
}
