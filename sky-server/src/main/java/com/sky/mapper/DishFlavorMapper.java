package com.sky.mapper;

import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author yw
 * @version 1.0
 * @description TODO
 * @createTime 2024/10/17 17:29
 */
@Mapper
public interface DishFlavorMapper {

    void insertBatch(List<DishFlavor> flavors);

    void deleteByDishIds(List<Long> dishIds);
}
