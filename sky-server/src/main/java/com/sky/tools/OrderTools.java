package com.sky.tools;

import com.sky.entity.AddressBook;
import com.sky.entity.Orders;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sky.vo.OrderCardVO;
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
    private final AddressBookMapper addressBookMapper;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 获取待处理订单列表（待接单、待派送）
     * 返回JSON字符串，前端会渲染为可操作的订单卡片
     * 限制返回最多5条，避免数据量过大导致AI处理超时
     */
    @Tool(description = "查询待处理订单列表，包括待接单和待派送的订单，返回JSON字符串供前端显示订单操作组件。最多返回5条最新订单。返回格式示例：{\"componentType\":\"ORDER_LIST\",\"title\":\"待处理订单\",\"total\":5,\"orders\":[{\"id\":1,\"orderNumber\":\"123\",\"status\":2,\"statusName\":\"待接单\",\"consignee\":\"张三\",\"phone\":\"13800138000\",\"address\":\"北京市\",\"amount\":100.0,\"orderTime\":\"2025-01-01T12:00:00\"}],\"actions\":[\"CONFIRM\",\"REJECT\",\"DELIVERY\"]}")
    public String getPendingOrders() {
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

            OrderCardVO vo = OrderCardVO.builder()
                    .componentType("ORDER_LIST")
                    .title("待处理订单")
                    .total(total)
                    .orders(convertToOrderInfoList(limitedOrders))
                    .actions(List.of("CONFIRM", "REJECT", "DELIVERY"))
                    .build();

            return objectMapper.writeValueAsString(vo);
        } catch (Exception e) {
            log.error("获取待处理订单失败", e);
            return "{\"componentType\":\"ORDER_LIST\",\"title\":\"待处理订单\",\"total\":0,\"orders\":[],\"actions\":[],\"message\":\"查询失败\"}";
        }
    }

    /**
     * 获取今日订单列表
     */
    @Tool(description = "查询今日所有订单，返回JSON字符串供前端显示订单列表组件")
    public String getTodayOrders() {
        log.info("AI查询今日订单");

        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startTime = LocalDateTime.of(today, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(today, LocalTime.MAX);

            List<Orders> orders = getOrdersByTimeRange(startTime, endTime);

            OrderCardVO vo = OrderCardVO.builder()
                    .componentType("ORDER_LIST")
                    .title("今日订单")
                    .total(orders.size())
                    .orders(convertToOrderInfoList(orders))
                    .actions(List.of("CONFIRM", "REJECT", "DELIVERY", "COMPLETE"))
                    .build();

            return objectMapper.writeValueAsString(vo);
        } catch (Exception e) {
            log.error("获取今日订单失败", e);
            return "{\"componentType\":\"ORDER_LIST\",\"title\":\"今日订单\",\"total\":0,\"orders\":[],\"actions\":[],\"message\":\"查询失败\"}";
        }
    }

    /**
     * 根据订单号查询订单详情
     */
    @Tool(description = "根据订单号查询订单详情，返回JSON字符串")
    public String getOrderByNumber(@ToolParam(description = "订单号，例如：1234567890") String orderNumber) {
        log.info("AI查询订单号：{}", orderNumber);

        try {
            Orders order = orderMapper.getByNumber(orderNumber);
            if (order == null) {
                OrderCardVO vo = OrderCardVO.builder()
                        .componentType("ORDER_LIST")
                        .title("订单查询结果")
                        .total(0)
                        .orders(List.of())
                        .message("未找到订单号：" + orderNumber)
                        .build();
                return objectMapper.writeValueAsString(vo);
            }

            OrderCardVO vo = OrderCardVO.builder()
                    .componentType("ORDER_LIST")
                    .title("订单详情")
                    .total(1)
                    .orders(List.of(convertToOrderInfo(order)))
                    .actions(getActionsByStatus(order.getStatus()))
                    .build();
            return objectMapper.writeValueAsString(vo);
        } catch (Exception e) {
            log.error("查询订单详情失败", e);
            return "{\"componentType\":\"ORDER_LIST\",\"title\":\"订单查询\",\"total\":0,\"orders\":[],\"actions\":[],\"message\":\"查询失败\"}";
        }
    }

    /**
     * 获取特定状态的订单
     */
    @Tool(description = "根据订单状态查询订单，状态：2-待接单，3-已接单(待派送)，4-派送中，5-已完成，返回JSON字符串")
    public String getOrdersByStatusCode(@ToolParam(description = "订单状态码：2待接单、3已接单、4派送中、5已完成") Integer status) {
        log.info("AI查询状态订单：{}", status);

        try {
            List<Orders> orders = getOrdersByStatus(status);

            String statusName = getStatusName(status);
            OrderCardVO vo = OrderCardVO.builder()
                    .componentType("ORDER_LIST")
                    .title(statusName + "订单")
                    .total(orders.size())
                    .orders(convertToOrderInfoList(orders))
                    .actions(getActionsByStatus(status))
                    .build();
            return objectMapper.writeValueAsString(vo);
        } catch (Exception e) {
            log.error("查询状态订单失败", e);
            return "{\"componentType\":\"ORDER_LIST\",\"title\":\"订单查询\",\"total\":0,\"orders\":[],\"actions\":[],\"message\":\"查询失败\"}";
        }
    }

    // ========== 私有辅助方法 ==========

    private List<Orders> getOrdersByStatus(Integer status) {
        return orderMapper.getListByStatus(status);
    }

    private List<Orders> getOrdersByTimeRange(LocalDateTime start, LocalDateTime end) {
        return orderMapper.getListByTimeRange(start, end);
    }

    private List<OrderCardVO.OrderInfo> convertToOrderInfoList(List<Orders> orders) {
        return orders.stream()
                .map(this::convertToOrderInfo)
                .collect(Collectors.toList());
    }

    private OrderCardVO.OrderInfo convertToOrderInfo(Orders order) {
        // 简化地址，只使用订单中已有的地址字段，避免查询地址簿
        String address = order.getAddress() != null ? order.getAddress() : "未填写地址";

        return OrderCardVO.OrderInfo.builder()
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

    /**
     * 根据 addressBookId 构建完整地址
     */
    private String buildAddress(Long addressBookId) {
        if (addressBookId == null) {
            return "未填写地址";
        }

        try {
            AddressBook addressBook = addressBookMapper.getById(addressBookId);
            if (addressBook == null) {
                return "地址信息不存在";
            }

            // 拼接完整地址：省 + 市 + 区 + 详细地址
            StringBuilder fullAddress = new StringBuilder();
            if (addressBook.getProvinceName() != null) {
                fullAddress.append(addressBook.getProvinceName());
            }
            if (addressBook.getCityName() != null) {
                fullAddress.append(addressBook.getCityName());
            }
            if (addressBook.getDistrictName() != null) {
                fullAddress.append(addressBook.getDistrictName());
            }
            if (addressBook.getDetail() != null) {
                fullAddress.append(addressBook.getDetail());
            }

            String result = fullAddress.toString();
            return result.isEmpty() ? "未填写地址" : result;

        } catch (Exception e) {
            log.error("查询地址簿失败, addressBookId={}", addressBookId, e);
            return "地址查询失败";
        }
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
