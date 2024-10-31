package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

/**
 * @author yw
 * @version 1.0
 * @description TODO
 * @createTime 2024/10/25 17:24
 */
public interface SetmealService {
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void saveWithDish(SetmealDTO setmealDTO);

    SetmealVO getByIdWithDish(Long id);

    void startOrStop(Integer status, Long id);
}