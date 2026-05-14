package com.langchain4j;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

public class PersistentChatMemoryStore implements ChatMemoryStore {
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        //根据memoryId去chat_msg查询内容，会话内容
        Entity chatMsg = Db.use().get(Entity.create("chat_msg").set("uid", memoryId));
        if(chatMsg != null){
            String message = chatMsg.getStr( "message");
            List<ChatMessage> chatMessages = messagesFromJson(message);
            return chatMessages;
        }else {
            return new ArrayList<>();
        }
        return List.of();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Entity chatMsg = Db.use().get(Entity.create("chat_msg").set("uid", memoryId));
        if(chatMsg !=null) {
            Db.use().update(
                    Entity.create().set("message", messagesToJson(messages)),//修改的数据
                    Entity.create("chat_msg").set("uid", memoryId) //where条件
            );
        }else{
            Db.use().insert(Entity.create("chat_msg").set("uid",memoryId)
                    .set("message",messagesToJson(messages)));
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Db.use().del(Entity.create("chat_msg").set("uid", memoryId));
    }
}
