package com.sky.service;

import com.sky.dto.DishDTO;

/**
 * @author yw
 * @version 1.0
 * @description TODO
 * @createTime 2024/10/17 17:22
 */
public interface DishService {
    void saveWithFlavor(DishDTO dishDTO);
}