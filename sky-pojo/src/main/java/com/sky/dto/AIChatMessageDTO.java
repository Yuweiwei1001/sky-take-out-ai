package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

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

    @ApiModelProperty(value = "对话ID", required = true)
    private String conversationId;

}