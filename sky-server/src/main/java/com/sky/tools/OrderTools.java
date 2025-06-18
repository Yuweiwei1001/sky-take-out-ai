package com.sky.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.result.OrderTableResponse;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author yw
 * @version 1.0
 * @description 订单工具类，用于大模型获取订单相关的数据
 * @createTime 2025/6/13 14:56
 */
@Component
@RequiredArgsConstructor
public class OrderTools {

    private final OrderService orderService;

    /**
     * 根据状态获取订单表格数据
     */
    @Tool(description = "根据订单状态获取订单列表，返回可操作的表格数据")
    public OrderTableResponse getOrdersTableByStatus(
            @JsonProperty("status")
            @JsonPropertyDescription("订单状态：1-待付款，2-待接单，3-已接单/待派送，4-派送中，5-已完成，6-已取消")
            Integer status) {

        OrdersPageQueryDTO queryDTO = new OrdersPageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(20);
        queryDTO.setStatus(status);

        PageResult pageResult = orderService.pageQuery4Admin(queryDTO);

        OrderTableResponse response = new OrderTableResponse(
                getStatusTitle(status),
                pageResult
        );


        return response;
    }

    private String getStatusTitle(Integer status) {
        switch (status) {
            case 1: return "待付款订单列表";
            case 2: return "待接单订单列表";
            case 3: return "已接单订单列表";
            case 4: return "派送中订单列表";
            case 5: return "已完成订单列表";
            case 6: return "已取消订单列表";
            default: return "订单列表";
        }
    }


    /**
     * 获取待接单的订单列表 - 返回结构化数据
     */
//    @Tool(description = "获取当前所有待接单的订单，返回表格数据供前端展示和操作")
//    public OrderTableResponse getPendingOrdersForTable() {
//        OrdersPageQueryDTO queryDTO = new OrdersPageQueryDTO();
//        queryDTO.setPage(1);
//        queryDTO.setPageSize(20);
//        queryDTO.setStatus(2); // 待接单状态
//
//        PageResult pageResult = orderService.pageQuery4Admin(queryDTO);
//
//        OrderTableResponse response = new OrderTableResponse(
//                "待接单订单列表",
//                pageResult
//        );
//
//        return response;
//    }

}