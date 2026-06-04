package com.langchain4j.service;

import cn.hutool.core.io.FileUtil;
import com.google.gson.Gson;
import com.langchain4j.aioutput.FoundRegisterOutput;
import com.langchain4j.aioutput.IntenttionOutput;
import com.langchain4j.aioutput.LostItemQueryOutput;
import com.langchain4j.aioutput.LostRegisterOutput;
import com.langchain4j.aiservice.AiIntentAssistant;
import com.langchain4j.aiservice.FoundItemRegisterAssistant;
import com.langchain4j.aiservice.LostItemQueryAssistant;
import com.langchain4j.aiservice.LostItemRegisterAssistant;
import com.langchain4j.aop.ChatHistoryAop;
import com.langchain4j.dto.ChatHistoryDTO;
import com.langchain4j.entity.ChatHistoryEntity;
import com.langchain4j.entity.FoundItemEntity;
import com.langchain4j.entity.LostItemEntity;
import com.langchain4j.repository.ChatHistoryRepository;
import com.langchain4j.repository.FoundItemRepository;
import com.langchain4j.repository.LostItemRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService{

    @Autowired
    private AiIntentAssistant aiAssistant;

    @Autowired
    private LostItemRegisterAssistant lostItemRegisterAssistant;
    @Autowired
    private FoundItemRegisterAssistant foundItemRegisterAssistant;
    @Autowired
    private LostItemQueryAssistant lostItemQueryAssistant;

    @Override
    @ChatHistoryAop
    public String chatStream(String userId, String message) {
        //用户意图判断（LangChain4j ChatMemory 会自动提供历史对话上下文）
        IntenttionOutput intenttionOutput=aiAssistant.aiIntention(userId, message);
        log.info("----------------------------------用户意图：{}",intenttionOutput);
        String output=intenttionOutput.getOutput();
        switch (intenttionOutput.getIntention()){
            case 1:
                //失物登记
                output=lostItemRegister(userId, message);
                break;
            case 2:
                //拾物登记
                output=foundItemRegister(userId, message);
                break;
            case 3:
                //失物查询
                output=lostItemQuery(userId, message);
                break;
            default:
                return output;
        }
        return output;
    }

    @Autowired
    private LostItemRepository lostItemRepository;
    public String lostItemRegister(String userId, String message) {
        LostRegisterOutput lostRegisterOutput=lostItemRegisterAssistant.lostItemRegister(userId, message);
        log.info("------------lostRegisterOutput：{}",lostRegisterOutput);
        if(lostRegisterOutput.getCompleted()){
            LostItemEntity lostItemEntity=new LostItemEntity();
            BeanUtils.copyProperties(lostRegisterOutput,lostItemEntity);
            lostItemRepository.save(lostItemEntity);
        }
        return lostRegisterOutput.getOutput();
    }

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;
    
    @Override
    public List<ChatHistoryDTO> queryChatHistory(String userId){
        List<ChatHistoryDTO> result=new ArrayList<>();
        List<ChatHistoryEntity>  entityList = chatHistoryRepository.findTop20BySessionIdOrderByIdDesc(userId);
        for (ChatHistoryEntity entity : entityList) {
            if(entity.getRole().equals("0")){
                ChatHistoryDTO dto=new ChatHistoryDTO();
                dto.setRole(entity.getRole());
                dto.setContent(entity.getContent());
                result.add(dto);
            }
        }
        return result;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearChatHistory(String userId){
        // 1. 从数据库删除
        chatHistoryRepository.deleteBySessionId(userId);
        
        log.info("已清除用户 {} 的所有聊天记录", userId);
    }

    @Autowired
    private FoundItemRepository foundItemRepository;
    
    @Autowired
    private Gson gson;
    
    public String foundItemRegister(String userId, String message) {
        // LangChain4j 自动应用 JSON Schema 约束，强制模型输出标准 JSON
        FoundRegisterOutput foundRegisterOutput = foundItemRegisterAssistant.foundItemRegister(userId, message);
        log.info("------------拾物登记结果：{}", foundRegisterOutput);
        // 如果完成登记，保存到数据库
        if (Boolean.TRUE.equals(foundRegisterOutput.getCompleted())) {
            FoundItemEntity foundItemEntity = new FoundItemEntity();
            BeanUtils.copyProperties(foundRegisterOutput, foundItemEntity);
            foundItemRepository.save(foundItemEntity);
        }

        return foundRegisterOutput.getOutput();
    }

    public String lostItemQuery(String userId, String message) {
        // 调用失物查询AI助手
        LostItemQueryOutput queryOutput = lostItemQueryAssistant.queryLostItem(userId, message);
        log.info("------------失物查询结果：{}", queryOutput);
        
        // 返回AI助手的输出给用户
        return queryOutput.getOutput();
    }


    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;
    @Autowired
    private EmbeddingModel embeddingModel;
    @Override
    public String embeddingIndex(){
        log.info("------将登记的拾得物信息整理成知识库、再写入向量数据库------");
        //将登记的拾得物数据查询出来写入到一个文件中
        String filePath = "D:\\lost-found.txt";
        FileUtil.writeString("",filePath, StandardCharsets.UTF_8);
        List<String> stringList= StreamSupport.stream(foundItemRepository.findAll().spliterator(),false).map(entity -> gson.toJson(entity)).toList();
        FileUtil.appendLines(stringList,filePath, StandardCharsets.UTF_8);
        //加载并解析文档
        DocumentParser documentParser = new TextDocumentParser();
        Document document = FileSystemDocumentLoader.loadDocument(FileUtil.getAbsolutePath(filePath), documentParser);
        //文档分割器分割文档
        DocumentSplitter documentSplitter = new DocumentByLineSplitter(200,100);
        List<TextSegment> split = documentSplitter.split(document);
        //获取向量
        Response<List<Embedding>> listResponse = embeddingModel.embedAll(split);
        //添加向量,存入向量数据库
        embeddingStore.addAll(listResponse.content(), split);
        return "success";
    }

    @Override
    public List<String> embeddingQuery(String message){
        //构建索引条件
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .query(message)
                .maxResults(2)
                .minScore(0.8)
                .build();
        //进行向量检索
        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(embeddingSearchRequest);
        //返回检索结果
        return search.matches().stream().map(match -> match.embedded().text()).collect(Collectors.toList());
    }
}
