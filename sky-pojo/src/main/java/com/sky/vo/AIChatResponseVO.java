package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "AI聊天响应视图对象")
public class AIChatResponseVO {

    @ApiModelProperty(value = "AI回复内容", required = true)
    private String reply;

    @ApiModelProperty(value = "对话ID", required = true)
    private String conversationId;

    @ApiModelProperty(value = "响应时间戳", required = true)
    private Long timestamp;
}