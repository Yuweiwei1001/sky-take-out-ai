package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author yw
 * @version 1.0
 * @description 订单状态定时处理任务
 * @createTime 2025/3/17 14:42
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     * 每分钟执行一次
     */
    @Scheduled(cron = "0 0/15 * * * ?")
    public void processTimeOutOrder(){
        log.info("处理超时订单,{}", LocalDateTime.now());
        // 当前时间减15分钟，查找订单时间在当前时间之前15分钟且未支付的订单
        // 将订单状态修改为“已取消”
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeOut(Orders.PENDING_PAYMENT, time);

        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders order : ordersList) {
                // 修改订单状态为“已取消”
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("订单超时取消");
                orderMapper.update(order);
                // TODO 发送消息到mq，通知用户订单已取消
            }
        }
    }

    /**
     * 处理待派送订单
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "* * 1 * * ?")
    public void processDeliveryOrder(){
        log.info("处理派送中订单,{}", LocalDateTime.now());
        // 查询待派送订单，订单状态为“派送中”且下单时间在当前时间之前24小时
        // 将订单状态修改为“完成”
        LocalDateTime time = LocalDateTime.now().minusHours(24);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeOut(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders order : ordersList) {
                // 修改订单状态为“完成”
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
                // TODO 发送消息到mq，通知用户订单已完成
            }
        }
    }
}