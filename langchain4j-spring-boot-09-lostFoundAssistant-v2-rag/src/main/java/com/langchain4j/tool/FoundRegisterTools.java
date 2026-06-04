package com.langchain4j.tool;

import com.langchain4j.entity.FoundItemEntity;
import com.langchain4j.entity.LostItemEntity;
import com.langchain4j.repository.FoundItemRepository;
import com.langchain4j.repository.LostItemRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class FoundRegisterTools {

    @Autowired
    private FoundItemRepository foundItemRepository;


    @Tool("根据联系电话查询拾取的物品信息")
    public List<FoundItemEntity> queryFoundItemByPhone(@P("用户联系电话")String phone) {
        log.info("根据联系电话查询拾取的物品信息，联系电话：{}",phone);
        return foundItemRepository.findAllByPhone(phone);
    }


}
