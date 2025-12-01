package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;

import static org.apache.commons.lang3.StringUtils.isNumeric;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 会话id
    private String conversationId;

    // 消息内容
    private String content;

    // 发送者：1-用户 2-AI
    private Integer senderType;

    // 发送时间
    private LocalDateTime sendTime;


    // SpringAI Message对象转换为ConversationMessage对象
    public static ConversationMessage fromSpringAIMessage(Message message, String conversationId) {
        ConversationMessage conversationMessage = new ConversationMessage();
        conversationMessage.setContent(message.getText());
        conversationMessage.setSendTime(LocalDateTime.now());
        conversationMessage.setConversationId(conversationId);


        // 根据消息角色设置发送者类型
        String role = message.getMessageType().name();
        log.info("消息角色: {}", role);
        if (role.equals("USER")) {
            conversationMessage.setSenderType(1); // 用户
        } else if (role.equals("ASSISTANT")) {
            conversationMessage.setSenderType(2); // AI
        } else {
            conversationMessage.setSenderType(0); // 系统消息或其他
        }

        return conversationMessage;
    }


    // ConversationMessage对象转换为SpringAI Message对象
    public Message toSpringAIMessage() {
        log.info("ConversationMessage:发送者类型 {}", senderType);
        if (senderType == 1)
            return new UserMessage( content);
        else
            return new AssistantMessage( content);
    }
}
