package com.sky.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * @author yw
 * @version 1.0
 * @description ai对话消息实体类
 * @createTime 2025/6/6 14:52
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息ID
     */
    private String id;


    /**
     * 消息角色：user-用户, assistant-AI助手
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    private Long timestamp;
    /**
     * 时间戳（用于前端显示）
     */
    public Long getTimestamp() {
        return createTime != null ?
                createTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() :
                null;
    }
}