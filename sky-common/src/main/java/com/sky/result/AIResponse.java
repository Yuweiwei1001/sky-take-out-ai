package com.sky.result;

import lombok.Data;

/**
 * @author yw
 * @version 1.0
 * @description ai响应结果封装类
 * @createTime 2025/6/13 15:25
 */
@Data
public class AIResponse {
    private Integer code;
    private String msg;
    private Object data;
    private String type; // 响应类型：text, table, chart 等
    private String conversationId;
    private Long timestamp;

    public static AIResponse success(Object data, String type) {
        AIResponse response = new AIResponse();
        response.setCode(1);
        response.setData(data);
        response.setType(type);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static AIResponse error(String message) {
        AIResponse response = new AIResponse();
        response.setCode(0);
        response.setMsg(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

}