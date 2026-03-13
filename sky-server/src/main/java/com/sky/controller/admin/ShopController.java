package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @author yw
 * @version 1.0
 * @description 店铺相关接口
 * @createTime 2024/11/21 16:11
 */
@RestController
@RequestMapping("/admin/shop")
@Slf4j
@Api(tags = "店铺相关接口")
public class ShopController {

    public static final String KEY = "SHOP_STATUS";
    @Autowired
    RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    @ApiOperation("营业/打烊店铺")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置营业状态为：{}",status);
        redisTemplate.opsForValue().set(KEY,status);
        return Result.success();
    }

    @GetMapping("/status")
    @ApiOperation("获取营业状态")
    public Result<Integer> getStatus(){
        Integer shop_status =(Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取营业状态为：{}",shop_status);
        return Result.success(shop_status);
    }
}