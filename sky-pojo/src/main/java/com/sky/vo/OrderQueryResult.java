package com.sky.vo;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单查询结构化输出结果
 * 用于 Spring AI Alibaba 结构化输出
 */
@JsonClassDescription("订单查询结果，包含订单列表和可操作信息")
@Builder
public record OrderQueryResult(
        @JsonPropertyDescription("组件类型，固定为 ORDER_LIST") 
        String componentType,
        
        @JsonPropertyDescription("查询结果标题") 
        String title,
        
        @JsonPropertyDescription("订单总数") 
        Integer total,
        
        @JsonPropertyDescription("订单详情列表") 
        List<OrderInfo> orders,
        
        @JsonPropertyDescription("可执行的操作列表，如 CONFIRM/REJECT/DELIVERY/COMPLETE") 
        List<String> actions,
        
        @JsonPropertyDescription("提示信息，如查询失败时的错误说明") 
        String message
) {
    public OrderQueryResult {
        if (componentType == null) {
            componentType = "ORDER_LIST";
        }
    }
    
    /**
     * 创建成功响应
     */
    public static OrderQueryResult success(String title, Integer total, List<OrderInfo> orders, List<String> actions) {
        return new OrderQueryResult("ORDER_LIST", title, total, orders, actions, null);
    }
    
    /**
     * 创建失败响应
     */
    public static OrderQueryResult error(String title, String message) {
        return new OrderQueryResult("ORDER_LIST", title, 0, List.of(), List.of(), message);
    }
    
    /**
     * 创建空结果响应
     */
    public static OrderQueryResult empty(String title, String message) {
        return new OrderQueryResult("ORDER_LIST", title, 0, List.of(), List.of(), message);
    }
    
    /**
     * 订单详情信息
     */
    @JsonClassDescription("单个订单的详细信息")
    @Builder
    public record OrderInfo(
            @JsonPropertyDescription("订单ID") 
            Long id,
            
            @JsonPropertyDescription("订单号") 
            String orderNumber,
            
            @JsonPropertyDescription("订单状态码：1待付款 2待接单 3已接单 4派送中 5已完成 6已取消") 
            Integer status,
            
            @JsonPropertyDescription("订单状态名称") 
            String statusName,
            
            @JsonPropertyDescription("收货人姓名") 
            String consignee,
            
            @JsonPropertyDescription("联系电话") 
            String phone,
            
            @JsonPropertyDescription("配送地址") 
            String address,
            
            @JsonPropertyDescription("订单金额") 
            BigDecimal amount,
            
            @JsonPropertyDescription("下单时间")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime orderTime
    ) {}
}
