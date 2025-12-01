package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.ConversationMessage;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConversationMessageMapper {

    void insert(ConversationMessage conversationMessage);


    List<ConversationMessage> getByConversationId(String conversationId);

    // 批量插入
//    void insertBatch(List<ConversationMessage> conversationMessages);
}
