package com.langchain4j.tool;

import com.langchain4j.entity.FoundItemEntity;
import com.langchain4j.entity.LostItemEntity;
import com.langchain4j.repository.FoundItemRepository;
import com.langchain4j.repository.LostItemRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LostItemQueryTools {

    @Autowired
    private FoundItemRepository foundItemRepository;
    
    @Autowired
    private LostItemRepository lostItemRepository;

    /**
     * 根据失主联系电话查询其登记的失物信息
     *//*
    @Tool("根据失主联系电话查询其登记的失物信息")
    public List<LostItemEntity> queryLostItemsByOwnerPhone(@P("失主联系电话") String phone) {
        log.info("根据失主联系电话查询失物信息，联系电话：{}", phone);
        return lostItemRepository.findAllByPhone(phone);
    }

    *//**
     * 根据物品名称查询拾物记录
     *//*
    @Tool("根据物品名称模糊查询拾物登记表中的记录")
    public List<FoundItemEntity> queryFoundItemsByName(@P("物品名称") String itemName) {
        log.info("根据物品名称查询拾物记录，物品名称：{}", itemName);
        return foundItemRepository.findByItemNameContaining(itemName);
    }*/

    @Autowired
    private ContentRetriever contentRetriever;
    @Tool("根据物品名称和特征描述查询登记的拾得物信息")
    public List<String> queryLostItemsByNameAndFeatures(@P("物品名称和特征描述") String desc) {
        log.info("根据丢失的物品名称和特征描述查询登记的拾得物信息，查询条件：{}", desc);
        List<String> result = contentRetriever.retrieve(new Query(desc)).stream().map(content -> content.textSegment().text()).toList();
        log.info("根据丢失的物品名称和特征描述查询登记的拾得物信息，查询结果：{}", result);
        return result;
    }

}