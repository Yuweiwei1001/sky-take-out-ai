package com.sky.result;

import lombok.Data;

/**
 * @author yw
 * @version 1.0
 * @description ai响应结果封装类
 * @createTime 2025/6/13 15:25
 */
@Data
public class AIResponse<T> {
    private Integer code;
    private String msg;
    private T data;
    private String type;
    private Long timestamp;

    public static <T> AIResponse<T> success(T data) {
        AIResponse<T> response = new AIResponse<>();
        response.setCode(1);
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static <T> AIResponse<T> error(String message) {
        AIResponse<T> response = new AIResponse<>();
        response.setCode(0);
        response.setMsg(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}
