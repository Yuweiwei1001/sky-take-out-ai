package com.sky.tools;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.vo.TurnoverReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;


/**
 * 营业数据工具类
 * 直接操作数据库查询营业额数据
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportTools {

    private final OrderMapper orderMapper;

    /**
     * 获取今日营业数据
     */
    @Tool(description = "获取今日营业数据，包括日期列表和营业额列表")
    public TurnoverReportVO getTodayTurnoverReport() {
        LocalDate today = LocalDate.now();
        return queryTurnoverByDateRange(today, today);
    }

    /**
     * 获取最近N天的营业数据
     */
    @Tool(description = "获取最近N天的营业数据，例如最近7天")
    public TurnoverReportVO getRecentTurnoverReport(
            @ToolParam(description = "最近的天数，例如：7表示最近7天") int days) {
        log.info("获取最近{}天的营业额数据", days);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        return queryTurnoverByDateRange(startDate, endDate);
    }

    /**
     * 获取昨日营业数据
     */
    @Tool(description = "获取昨日营业数据")
    public TurnoverReportVO getYesterdayTurnoverReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return queryTurnoverByDateRange(yesterday, yesterday);
    }

    /**
     * 获取本月营业数据
     */
    @Tool(description = "获取本月营业数据")
    public TurnoverReportVO getCurrentMonthTurnoverReport() {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        return queryTurnoverByDateRange(firstDayOfMonth, lastDayOfMonth);
    }

    /**
     * 获取上月营业数据
     */
    @Tool(description = "获取上月营业数据")
    public TurnoverReportVO getLastMonthTurnoverReport() {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfLastMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayOfLastMonth = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth());

        return queryTurnoverByDateRange(firstDayOfLastMonth, lastDayOfLastMonth);
    }

    /**
     * 获取本年度营业数据
     */
    @Tool(description = "获取本年度营业数据")
    public TurnoverReportVO getCurrentYearTurnoverReport() {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfYear = now.withDayOfYear(1);
        LocalDate lastDayOfYear = now.withDayOfYear(now.lengthOfYear());

        return queryTurnoverByDateRange(firstDayOfYear, lastDayOfYear);
    }

    /**
     * 获取指定月份的营业数据
     */
    @Tool(description = "获取指定年份和月份的营业数据")
    public TurnoverReportVO getMonthlyTurnoverReport(
            @ToolParam(description = "年份，例如：2025") int year,
            @ToolParam(description = "月份，1-12之间的数字，例如：6表示6月") int month) {

        try {
            LocalDate firstDay = LocalDate.of(year, month, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

            return queryTurnoverByDateRange(firstDay, lastDay);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期参数错误，请检查年份和月份");
        }
    }

    /**
     * 获取本周营业数据
     */
    @Tool(description = "获取本周营业数据（周一到周日）")
    public TurnoverReportVO getCurrentWeekTurnoverReport() {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = now.with(DayOfWeek.SUNDAY);

        return queryTurnoverByDateRange(startOfWeek, endOfWeek);
    }

    /**
     * 获取指定日期范围的营业数据
     */
    @Tool(description = "获取指定日期范围的营业数据，日期格式：yyyy-MM-dd")
    public TurnoverReportVO getTurnoverReportByDateRange(
            @ToolParam(description = "开始日期，格式：yyyy-MM-dd，例如：2025-01-01") String startDate,
            @ToolParam(description = "结束日期，格式：yyyy-MM-dd，例如：2025-01-31") String endDate) {

        try {
            log.info("获取指定日期的营业额，开始日期：{}，结束日期：{}", startDate, endDate);
            LocalDate begin = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            return queryTurnoverByDateRange(begin, end);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式错误，请使用 yyyy-MM-dd 格式");
        }
    }

    /**
     * 核心方法：直接查询数据库获取营业额数据
     */
    private TurnoverReportVO queryTurnoverByDateRange(LocalDate begin, LocalDate end) {
        // 生成日期列表
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        String dateListStr = StringUtils.join(dateList, ",");

        // 查询每日营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 直接查询数据库
            Double turnover = orderMapper.getTurnoverSumByDate(beginTime, endTime, Orders.COMPLETED);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        String turnoverListStr = StringUtils.join(turnoverList, ",");

        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }
}
