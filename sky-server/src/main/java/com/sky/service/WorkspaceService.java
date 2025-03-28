package com.sky.service;

import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;

import java.time.LocalDateTime;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2025/3/28 10:59
 */
public interface WorkspaceService {

    // 获取今日营业数据
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);

    SetmealOverViewVO getSetmealOverView();

    DishOverViewVO getDishOverView();

    OrderOverViewVO getOrdersOverView();
}
