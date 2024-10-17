package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author yw
 * @version 1.0
 * @description 自定义注解，用于标记需要自动填充操作的字段或方法
 * @createTime 2024/10/17 14:41
 */
@Target(ElementType.METHOD) //定义了该注解只能应用于方法
@Retention(RetentionPolicy.RUNTIME)//f指定了该注解在运行时仍然有效，使得可以在运行时通过反射机制获取注解信息
public @interface AutoFill {
    OperationType value();

}