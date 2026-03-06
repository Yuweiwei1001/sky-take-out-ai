package com.sky.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期时间工具类
 * 为 AI 助手提供日期时间相关的工具函数
 */
@Component
@Slf4j
public class DateTimeTools {

    /**
     * 获取当前日期时间
     */
    @Tool(description = "获取当前日期时间，格式：yyyy-MM-dd HH:mm:ss")
    public String getCurrentDateTime() {
        log.info("工具调用：获取当前日期时间");
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 获取当前日期
     */
    @Tool(description = "获取当前日期，格式：yyyy-MM-dd")
    public String getCurrentDate() {
        log.info("工具调用：获取当前日期");
        LocalDate now = LocalDate.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * 获取当前时间
     */
    @Tool(description = "获取当前时间，格式：HH:mm:ss")
    public String getCurrentTime() {
        log.info("工具调用：获取当前时间");
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * 获取当前年份
     */
    @Tool(description = "获取当前年份")
    public int getCurrentYear() {
        log.info("工具调用：获取当前年份");
        return LocalDate.now().getYear();
    }

    /**
     * 获取当前月份
     */
    @Tool(description = "获取当前月份，1-12")
    public int getCurrentMonth() {
        log.info("工具调用：获取当前月份");
        return LocalDate.now().getMonthValue();
    }

    /**
     * 获取今天是星期几
     */
    @Tool(description = "获取今天是星期几（中文）")
    public String getCurrentDayOfWeek() {
        log.info("工具调用：获取今天是星期几");
        String[] weekdays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        return weekdays[dayOfWeek - 1];
    }

    /**
     * 计算日期差
     */
    @Tool(description = "计算两个日期之间的天数差")
    public long calculateDaysBetween(
            @ToolParam(description = "开始日期，格式：yyyy-MM-dd") String startDate,
            @ToolParam(description = "结束日期，格式：yyyy-MM-dd") String endDate) {
        log.info("工具调用：计算日期差，{} 到 {}", startDate, endDate);
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return ChronoUnit.DAYS.between(start, end);
        } catch (Exception e) {
            log.error("日期解析失败", e);
            return -1L;
        }
    }
}
