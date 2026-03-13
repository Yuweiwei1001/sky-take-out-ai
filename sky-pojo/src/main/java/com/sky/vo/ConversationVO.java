package com.sky.vo;

import com.sky.entity.Conversation;
import com.sky.entity.ConversationMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;


/**
 * 会话视图对象
 */
// 在 ConversationVO 类上添加 @SuperBuilder 注解
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO extends Conversation implements Serializable {
    private List<ConversationMessage> messages;
}

