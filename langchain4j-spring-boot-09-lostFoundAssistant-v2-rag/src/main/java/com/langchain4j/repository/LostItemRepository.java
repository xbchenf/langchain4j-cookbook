package com.langchain4j.repository;

import com.langchain4j.entity.LostItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LostItemRepository extends JpaRepository<LostItemEntity, Long> {

    List<LostItemEntity> findAllByPhone(String phone);
}
