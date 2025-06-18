package com.sky.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;


/**
 * @author yw
 * @version 1.0
 * @description 营业数据工具类，用于大模型获取营业额数据
 * @createTime 2025/6/12 15:38
 */

@Component
@RequiredArgsConstructor
public class ReportTools {


    private final ReportService reportService;

    /**
     * 获取今日营业数据
     */
    /**
     * 获取今日营业数据
     */
    @Tool(description = "获取今日的营业数据，包括营业额")
    public TurnoverReportVO getTodayTurnoverReport() {
        LocalDate today = LocalDate.now();
        return reportService.getTurnoverReport(today, today);
    }

    /**
     * 获取指定日期范围的营业数据
     */
    @Tool(description ="获取指定时间范围内的营业数据，需要提供开始日期和结束日期")
    public TurnoverReportVO getTurnoverReportByDateRange(
            @JsonProperty("startDate")
            @JsonPropertyDescription("开始日期，格式：yyyy-MM-dd，例如：2025-01-01")
            String startDate,

            @JsonProperty("endDate")
            @JsonPropertyDescription("结束日期，格式：yyyy-MM-dd，例如：2025-01-31")
            String endDate) {

        try {
            LocalDate begin = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            return reportService.getTurnoverReport(begin, end);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式错误，请使用 yyyy-MM-dd 格式");
        }
    }

    /**
     * 获取最近N天的营业数据
     */
    @Tool(description ="获取最近几天的营业数据汇总")
    public TurnoverReportVO getRecentTurnoverReport(
            @JsonProperty("days")
            @JsonPropertyDescription("最近的天数，例如：7表示最近7天")
            int days) {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        return reportService.getTurnoverReport(startDate, endDate);
    }

    /**
     * 获取昨日营业数据
     */
    @Tool(description ="获取昨天的营业数据")
    public TurnoverReportVO getYesterdayTurnoverReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return reportService.getTurnoverReport(yesterday, yesterday);
    }

    /**
     * 获取本月营业数据
     */
    @Tool(description ="获取当前月份的营业数据")
    public TurnoverReportVO getCurrentMonthTurnoverReport() {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        return reportService.getTurnoverReport(firstDayOfMonth, lastDayOfMonth);
    }

    /**
     * 获取上月营业数据
     */
    @Tool(description ="获取上个月的营业数据")
    public TurnoverReportVO getLastMonthTurnoverReport() {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfLastMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayOfLastMonth = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth());

        return reportService.getTurnoverReport(firstDayOfLastMonth, lastDayOfLastMonth);
    }

    /**
     * 获取本年度营业数据
     */
    @Tool(description ="获取当前年度的营业数据")
    public TurnoverReportVO getCurrentYearTurnoverReport() {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfYear = now.withDayOfYear(1);
        LocalDate lastDayOfYear = now.withDayOfYear(now.lengthOfYear());

        return reportService.getTurnoverReport(firstDayOfYear, lastDayOfYear);
    }

    /**
     * 获取指定月份的营业数据
     */
    @Tool(description ="获取指定年月的营业数据")
    public TurnoverReportVO getMonthlyTurnoverReport(
            @JsonProperty("year")
            @JsonPropertyDescription("年份，例如：2025")
            int year,

            @JsonProperty("month")
            @JsonPropertyDescription("月份，1-12之间的数字，例如：6表示6月")
            int month) {

        try {
            LocalDate firstDay = LocalDate.of(year, month, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

            return reportService.getTurnoverReport(firstDay, lastDay);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期参数错误，请检查年份和月份");
        }
    }

    /**
     * 获取本周营业数据
     */
    @Tool(description ="获取本周（周一到周日）的营业数据")
    public TurnoverReportVO getCurrentWeekTurnoverReport() {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = now.with(DayOfWeek.SUNDAY);

        return reportService.getTurnoverReport(startOfWeek, endOfWeek);
    }
}