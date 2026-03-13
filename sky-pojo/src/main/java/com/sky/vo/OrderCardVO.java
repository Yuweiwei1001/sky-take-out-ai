package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单卡片视图对象
 * 用于AI返回结构化数据，前端渲染为可交互的订单组件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCardVO implements Serializable {

    /**
     * 组件类型，前端根据此字段识别渲染何种组件
     * 如：ORDER_LIST - 订单列表组件
     */
    private String componentType;

    /**
     * 组件标题
     */
    private String title;

    /**
     * 订单总数
     */
    private Integer total;

    /**
     * 订单列表
     */
    private List<OrderInfo> orders;

    /**
     * 支持的操作按钮
     * CONFIRM-接单, REJECT-拒单, DELIVERY-派送, COMPLETE-完成
     */
    private List<String> actions;

    /**
     * 提示消息（如查询为空时）
     */
    private String message;

    /**
     * 单个订单信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderInfo implements Serializable {
        /**
         * 订单ID
         */
        private Long id;

        /**
         * 订单号
         */
        private String orderNumber;

        /**
         * 订单状态
         */
        private Integer status;

        /**
         * 状态名称
         */
        private String statusName;

        /**
         * 收货人
         */
        private String consignee;

        /**
         * 手机号
         */
        private String phone;

        /**
         * 地址
         */
        private String address;

        /**
         * 订单金额
         */
        private BigDecimal amount;

        /**
         * 下单时间
         */
        private LocalDateTime orderTime;

        /**
         * 备注
         */
        private String remark;
    }
}
