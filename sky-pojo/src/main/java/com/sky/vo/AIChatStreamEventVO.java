package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatStreamEventVO {

    private String type;

    private String conversationId;

    private String content;

    private String message;

    private LocalDateTime replyTime;
}