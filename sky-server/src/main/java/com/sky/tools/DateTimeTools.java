package com.sky.tools;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2025/6/3 11:14
 */

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Function;


public class DateTimeTools {

    @Tool(description = "Get the current date and time in the user's timezone")
    public String getCurrentDateTimeFunction() {
        System.out.println("function calling:获取今日时间");
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
}