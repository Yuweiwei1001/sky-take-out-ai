package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2025/3/28 11:00
 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public BusinessDataVO getBusinessData() {
        LocalDate date = LocalDate.now();
        LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
        //查询今日营业额
        Double turnover = orderMapper.getTurnoverSumByDate(beginTime, endTime, Orders.COMPLETED);
        turnover = turnover == null ? 0.0 : turnover;

        //查询今日有效订单数
        Integer validOrderCount = orderMapper.countByDate(beginTime, endTime, Orders.COMPLETED);
        validOrderCount = validOrderCount == null ? 0 : validOrderCount;
        //计算平均客单价，营业额/有效订单数
        Double unitPrice = 0.0;
        if (validOrderCount > 0) {
            unitPrice = turnover/ validOrderCount.doubleValue();
        }
        
        //查询今日总订单数
        Integer totalOrderCount = orderMapper.countByDate(beginTime, endTime, null);
        totalOrderCount = totalOrderCount == null ? 0 : totalOrderCount;
        //计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount > 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();
        }
        //查询新增用户数
        Integer newUsers = userMapper.countByDate(beginTime, endTime);
        return BusinessDataVO
                .builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .unitPrice(unitPrice)
                .orderCompletionRate(orderCompletionRate)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 查询起售/停售套餐
     */
    @Override
    public SetmealOverViewVO getSetmealOverView() {
        Integer discontinued = setmealMapper.countByStatus(0);//停售
        Integer sold = setmealMapper.countByStatus(1);//起售
        return SetmealOverViewVO
                .builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();
    }

    @Override
    public DishOverViewVO getDishOverView() {
        Integer discontinued = dishMapper.countByStatus(0);//停售
        Integer sold = dishMapper.countByStatus(1);//起售
        return DishOverViewVO
                .builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();
    }

    /**
     * 查询全部订单、已取消数量、已完成数量、待派送数量、待接单数量
     */
    @Override
    public OrderOverViewVO getOrdersOverView() {
        Integer allOrders = orderMapper.countByStatus(null);
        Integer cancelledOrders = orderMapper.countByStatus(Orders.CANCELLED);
        Integer completedOrders = orderMapper.countByStatus(Orders.COMPLETED);
        Integer deliveredOrders = orderMapper.countByStatus(Orders.CONFIRMED);
        Integer waitingOrders = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        return OrderOverViewVO
                .builder()
                .allOrders(allOrders)
                .cancelledOrders(cancelledOrders)
                .completedOrders(completedOrders)
                .deliveredOrders(deliveredOrders)
                .waitingOrders(waitingOrders)
                .build();
    }
}