package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;

import java.util.List;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2024/10/17 17:22
 */
public interface DishService {
    void saveWithFlavor(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /*
    * @description 根据id查询菜品和对应的口味数据
    * @author yw
    * @date 2024/10/21 16:23
    * @param
    * @return
    */
    void deleteByIds(List<Long> ids);
}