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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 营业数据工具类
 * 直接操作数据库查询营业额数据
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportTools {

    private final OrderMapper orderMapper;

    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}).*?(\\d{4}-\\d{2}-\\d{2})");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 通用营业额查询工具（支持自然语言日期描述）
     */
    @Tool(description = "查询营业额数据。支持多种日期描述：今天、昨天、本周、上周、本月、上月、今年、去年、最近N天（如最近7天）、具体月份（如2025年1月）、日期范围（如2025-01-01到2025-01-31）")
    public TurnoverReportVO getTurnoverReport(
            @ToolParam(description = "日期范围描述，例如：今天、昨天、本周、本月、上月、最近7天、最近30天、2025年1月、2025-01-01到2025-01-31") String dateRange) {
        log.info("查询营业额，日期描述：{}", dateRange);
        DateRange range = parseDateRange(dateRange);
        return queryTurnoverByDateRange(range.begin, range.end);
    }

    /**
     * 解析自然语言日期描述为日期范围
     */
    private DateRange parseDateRange(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("日期描述不能为空");
        }
        String desc = description.trim().toLowerCase();
        LocalDate today = LocalDate.now();

        // 今天
        if (desc.contains("今天") || desc.equals("today")) {
            return new DateRange(today, today);
        }
        // 昨天
        if (desc.contains("昨天") || desc.equals("yesterday")) {
            LocalDate yesterday = today.minusDays(1);
            return new DateRange(yesterday, yesterday);
        }
        // 本周
        if (desc.contains("本周") || desc.contains("这周") || desc.equals("this week")) {
            LocalDate startOfWeek = today.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
            LocalDate endOfWeek = startOfWeek.plusDays(6);
            return new DateRange(startOfWeek, endOfWeek);
        }
        // 上周
        if (desc.contains("上周") || desc.equals("last week")) {
            LocalDate startOfThisWeek = today.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
            LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
            LocalDate endOfLastWeek = startOfLastWeek.plusDays(6);
            return new DateRange(startOfLastWeek, endOfLastWeek);
        }
        // 本月
        if (desc.contains("本月") || desc.contains("这个月") || desc.equals("this month")) {
            LocalDate firstDay = today.withDayOfMonth(1);
            LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
            return new DateRange(firstDay, lastDay);
        }
        // 上月
        if (desc.contains("上月") || desc.contains("上个月") || desc.equals("last month")) {
            LocalDate firstDayOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
            LocalDate lastDayOfLastMonth = today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth());
            return new DateRange(firstDayOfLastMonth, lastDayOfLastMonth);
        }
        // 今年
        if (desc.contains("今年") || desc.equals("this year")) {
            LocalDate firstDay = today.withDayOfYear(1);
            LocalDate lastDay = today.withDayOfYear(today.lengthOfYear());
            return new DateRange(firstDay, lastDay);
        }
        // 去年
        if (desc.contains("去年") || desc.equals("last year")) {
            LocalDate firstDay = today.minusYears(1).withDayOfYear(1);
            LocalDate lastDay = today.minusYears(1).withDayOfYear(today.minusYears(1).lengthOfYear());
            return new DateRange(firstDay, lastDay);
        }
        // 最近N天
        if (desc.matches(".*最近\\s*\\d+\\s*天.*") || desc.matches("last\\s*\\d+\\s*days")) {
            int days = extractNumber(desc);
            LocalDate startDate = today.minusDays(days - 1);
            return new DateRange(startDate, today);
        }
        // 具体月份，如 "2025年1月" 或 "2025-01"
        if (desc.matches("\\d{4}年\\d{1,2}月") || desc.matches("\\d{4}-\\d{1,2}")) {
            return parseMonthDescription(desc);
        }
        // 日期范围，如 "2025-01-01到2025-01-31" 或 "2025-01-01 至 2025-01-31"
        Matcher matcher = DATE_RANGE_PATTERN.matcher(desc);
        if (matcher.find()) {
            LocalDate begin = LocalDate.parse(matcher.group(1), DATE_FORMATTER);
            LocalDate end = LocalDate.parse(matcher.group(2), DATE_FORMATTER);
            return new DateRange(begin, end);
        }

        throw new IllegalArgumentException("无法识别的日期描述：" + description + "，支持的格式如：今天、本周、本月、最近7天、2025年1月、2025-01-01到2025-01-31");
    }

    /**
     * 解析月份描述
     */
    private DateRange parseMonthDescription(String desc) {
        try {
            String normalized = desc.replace("年", "-").replace("月", "");
            String[] parts = normalized.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            LocalDate firstDay = LocalDate.of(year, month, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
            return new DateRange(firstDay, lastDay);
        } catch (Exception e) {
            throw new IllegalArgumentException("月份格式错误：" + desc);
        }
    }

    /**
     * 从字符串中提取数字
     */
    private int extractNumber(String desc) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(desc);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        throw new IllegalArgumentException("无法从描述中提取天数：" + desc);
    }

    /**
     * 日期范围内部类
     */
    private record DateRange(LocalDate begin, LocalDate end) {}

    /**
     * 核心方法：直接查询数据库获取营业额数据（单次查询整个区间）
     */
    private TurnoverReportVO queryTurnoverByDateRange(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 单次查询整个区间的营业额总和
        Double turnover = orderMapper.getTurnoverSumByDate(beginTime, endTime, Orders.COMPLETED);
        turnover = turnover == null ? 0.0 : turnover;

        // 生成日期范围字符串
        String dateRangeStr = begin.equals(end) ? begin.toString() : begin + "到" + end;

        log.info("营业额数据，日期范围：{}，营业额：{}", dateRangeStr, turnover);
        return TurnoverReportVO.builder()
                .dateList(dateRangeStr)
                .turnoverList(String.valueOf(turnover))
                .build();
    }
}
