package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author yw
 * @version 1.0
 * @description 智能对话消息DTO
 * @createTime 2025/6/6 14:49
 */
@Data
@ApiModel(description = "AI聊天发送消息请求DTO")
public class AIChatMessageDTO {
    @ApiModelProperty(value = "用户消息内容", required = true)
    private String message;

    @ApiModelProperty(value = "对话ID")
    private String conversationId;

    @ApiModelProperty(value = "发送时间")
    private String sendTime;

}