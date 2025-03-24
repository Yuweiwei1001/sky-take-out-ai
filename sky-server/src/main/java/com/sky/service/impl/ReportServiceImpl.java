package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2025/3/24 11:26
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * @description 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return com.sky.vo.TurnoverReportVO
     * @author yw
     * @date 2025/3/24 11:26
     */
    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        // 先将日期一天一天加入到list，再将list转为字符串，逗号分隔
        List<LocalDate> dateList = new ArrayList<>();
        // 遍历日期
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        //将list转为字符串，逗号分隔
        String dateListStr = StringUtils.join(dateList, ",");
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 获取当日营业额
            // 查询当日开始时间2025年3月24日0点0分0秒和结束时间23点59分59.999...秒
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Double turnover = orderMapper.getTurnoverSumByDate(beginTime, endTime, Orders.COMPLETED);
            turnover = turnover == null ? 0.0 : turnover; // 如果turnover为null，则设置为0.0
            // 将营业额加入到turnoverList
            turnoverList.add(turnover);
        }
        String turnoverListStr = StringUtils.join(turnoverList, ",");
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        // 遍历日期
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        //将list转为字符串，逗号分隔
        String dateListStr = StringUtils.join(dateList, ",");

        //统计用户总量列表
        List<Integer> totalUserList = new ArrayList<>();
        // 统计新增用户列表
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 获取当日用户总量
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer totalUser = userMapper.countByDate(null,endTime);
            totalUser = totalUser == null ? 0 : totalUser;
            totalUserList.add(totalUser);

            // 获取当日新增用户量
            Integer newUser = userMapper.countByDate(beginTime,endTime);
            newUser = newUser == null ? 0 : newUser;
            newUserList.add(newUser);
        }
        String totalUserListStr = StringUtils.join(totalUserList, ",");
        String newUserListStr = StringUtils.join(newUserList, ",");
        return UserReportVO.builder()
                .dateList(dateListStr)
                .totalUserList(totalUserListStr)
                .newUserList(newUserListStr)
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        String dateListStr = StringUtils.join(dateList, ",");
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        List<Double> orderCompletionRateList = new ArrayList<>();

        // 遍历日期, 获取当日订单总数量，有效订单数量，订单完成率
        for (LocalDate date : dateList) {
            // 获取当日订单总数量
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = orderMapper.countByDate(beginTime, endTime,null);
            orderCount = orderCount == null ? 0 : orderCount;
            orderCountList.add(orderCount);

            // 获取当日有效订单数量(已完成的订单)
            Integer validOrderCount = orderMapper.countByDate(beginTime, endTime, Orders.COMPLETED);
            validOrderCount = validOrderCount == null ? 0 : validOrderCount;
            validOrderCountList.add(validOrderCount);
        }

        // 计算时间区间内的订单总数、有效订单总数和订单完成率
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        Double orderCompletionRate = 0.0;
        if (totalOrderCount > 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();
        }

        String orderCountListStr = StringUtils.join(orderCountList, ",");
        String validOrderCountListStr = StringUtils.join(validOrderCountList, ",");


        return OrderReportVO.builder()
                .dateList(dateListStr)
                .orderCountList(orderCountListStr)
                .validOrderCountList(validOrderCountListStr)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }
}