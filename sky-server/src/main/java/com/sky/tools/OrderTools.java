package com.sky.tools;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.vo.OrderQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单查询工具类
 * 为 AI 助手提供订单查询功能，返回结构化数据供前端渲染组件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTools {

    private final OrderMapper orderMapper;

    /**
     * 获取待处理订单列表（待接单、待派送）
     * 返回结构化数据，前端会渲染为可操作的订单卡片
     * 限制返回最多5条，避免数据量过大导致AI处理超时
     */
    @Tool(description = "查询待处理订单列表，包括待接单和待派送的订单，返回结构化数据供前端显示订单操作组件。最多返回5条最新订单。")
    public OrderQueryResult getPendingOrders() {
        log.info("AI查询待处理订单");

        try {
            // 待接单状态 = 2
            List<Orders> toBeConfirmed = getOrdersByStatus(Orders.TO_BE_CONFIRMED);
            // 待派送状态 = 3
            List<Orders> confirmed = getOrdersByStatus(Orders.CONFIRMED);

            List<Orders> allPending = new ArrayList<>();
            allPending.addAll(toBeConfirmed);
            allPending.addAll(confirmed);

            // 按时间倒序
            allPending.sort((a, b) -> b.getOrderTime().compareTo(a.getOrderTime()));

            // 限制最多返回5条，避免数据量过大
            int total = allPending.size();
            List<Orders> limitedOrders = allPending.stream()
                    .limit(5)
                    .collect(Collectors.toList());

            return OrderQueryResult.success(
                    "待处理订单",
                    total,
                    convertToOrderInfoList(limitedOrders),
                    List.of("CONFIRM", "REJECT", "DELIVERY")
            );
        } catch (Exception e) {
            log.error("获取待处理订单失败", e);
            return OrderQueryResult.error("待处理订单", "查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取今日订单列表
     */
    @Tool(description = "查询今日所有订单，返回结构化数据供前端显示订单列表组件")
    public OrderQueryResult getTodayOrders() {
        log.info("AI查询今日订单");

        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startTime = LocalDateTime.of(today, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(today, LocalTime.MAX);

            List<Orders> orders = getOrdersByTimeRange(startTime, endTime);

            return OrderQueryResult.success(
                    "今日订单",
                    orders.size(),
                    convertToOrderInfoList(orders),
                    List.of("CONFIRM", "REJECT", "DELIVERY", "COMPLETE")
            );
        } catch (Exception e) {
            log.error("获取今日订单失败", e);
            return OrderQueryResult.error("今日订单", "查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据订单号查询订单详情
     */
    @Tool(description = "根据订单号查询订单详情，返回结构化数据")
    public OrderQueryResult getOrderByNumber(@ToolParam(description = "订单号，例如：1234567890") String orderNumber) {
        log.info("AI查询订单号：{}", orderNumber);

        try {
            Orders order = orderMapper.getByNumber(orderNumber);
            if (order == null) {
                return OrderQueryResult.empty("订单查询结果", "未找到订单号：" + orderNumber);
            }

            return OrderQueryResult.success(
                    "订单详情",
                    1,
                    List.of(convertToOrderInfo(order)),
                    getActionsByStatus(order.getStatus())
            );
        } catch (Exception e) {
            log.error("查询订单详情失败", e);
            return OrderQueryResult.error("订单查询", "查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取特定状态的订单
     */
    @Tool(description = "根据订单状态查询订单，状态：2-待接单，3-已接单(待派送)，4-派送中，5-已完成，返回结构化数据")
    public OrderQueryResult getOrdersByStatusCode(@ToolParam(description = "订单状态码：2待接单、3已接单、4派送中、5已完成") Integer status) {
        log.info("AI查询状态订单：{}", status);

        try {
            List<Orders> orders = getOrdersByStatus(status);

            String statusName = getStatusName(status);
            return OrderQueryResult.success(
                    statusName + "订单",
                    orders.size(),
                    convertToOrderInfoList(orders),
                    getActionsByStatus(status)
            );
        } catch (Exception e) {
            log.error("查询状态订单失败", e);
            return OrderQueryResult.error("订单查询", "查询失败：" + e.getMessage());
        }
    }

    // ========== 私有辅助方法 ==========

    private List<Orders> getOrdersByStatus(Integer status) {
        return orderMapper.getListByStatus(status);
    }

    private List<Orders> getOrdersByTimeRange(LocalDateTime start, LocalDateTime end) {
        return orderMapper.getListByTimeRange(start, end);
    }

    private List<OrderQueryResult.OrderInfo> convertToOrderInfoList(List<Orders> orders) {
        return orders.stream()
                .map(this::convertToOrderInfo)
                .collect(Collectors.toList());
    }

    private OrderQueryResult.OrderInfo convertToOrderInfo(Orders order) {
        // 简化地址，只使用订单中已有的地址字段，避免查询地址簿
        String address = order.getAddress() != null ? order.getAddress() : "未填写地址";

        return OrderQueryResult.OrderInfo.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .status(order.getStatus())
                .statusName(getStatusName(order.getStatus()))
                .consignee(order.getConsignee())
                .phone(order.getPhone())
                .address(address)
                .amount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
    }

    private String getStatusName(Integer status) {
        return switch (status) {
            case 1 -> "待付款";
            case 2 -> "待接单";
            case 3 -> "已接单";
            case 4 -> "派送中";
            case 5 -> "已完成";
            case 6 -> "已取消";
            default -> "未知";
        };
    }

    private List<String> getActionsByStatus(Integer status) {
        return switch (status) {
            case 2 -> List.of("CONFIRM", "REJECT"); // 待接单：可接单、拒单
            case 3 -> List.of("DELIVERY"); // 已接单：可派送
            case 4 -> List.of("COMPLETE"); // 派送中：可完成
            default -> List.of();
        };
    }
}
