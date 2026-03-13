package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2025/3/28 10:55
 */
@RestController
@RequestMapping("/admin/workspace")
@Api(tags = "工作台相关接口")
@Slf4j
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;
    /*
     * 获取今日运营数据
     */
    @GetMapping("/businessData")
    public Result<BusinessDataVO> getBusinessData(){
        //获得当天的开始时间
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
        //获得当天的结束时间
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);

        return Result.success(workspaceService.getBusinessData(begin, end));
    }

    /**
     * 获取套餐总览数据
     *
     */

    @GetMapping("/overviewSetmeals")
    public Result<SetmealOverViewVO> getSetmealOverView(){
        return Result.success(workspaceService.getSetmealOverView());
    }

    /**
     * 获取菜品总览数据
     */
    @GetMapping("/overviewDishes")
    public Result<DishOverViewVO> getDishOverView(){
        return Result.success(workspaceService.getDishOverView());
    }

    /**
     * 获取订单总览数据
     */
    @GetMapping("/overviewOrders")
    public Result<OrderOverViewVO> getOrdersOverView(){
        return Result.success(workspaceService.getOrdersOverView());
    }


}