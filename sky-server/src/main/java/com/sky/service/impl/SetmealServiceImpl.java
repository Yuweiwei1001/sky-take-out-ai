package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2024/10/25 17:24
 */
@Service
public class SetmealServiceImpl implements SetmealService{

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        //保存套餐基本数据
        setmealMapper.insert(setmeal);
        //获取套餐的id，赋值给套餐菜品对象
        Long setmealId = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes!=null&&setmealDishes.size()>0){
            //为每个套餐菜品设置套餐ID
            setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
            //批量插入套餐菜品数据
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    @Override
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Override
    @Transactional
    public void startOrStop(Integer status, Long SetmealId) {
        //根据套餐id查询出套餐中的菜品，检查其状态，套餐含未起售菜品则不能起售
        List<Dish> dishes = setmealDishMapper.getDishesBySetmealId(SetmealId);
        for (Dish dish : dishes) {
            if(dish.getStatus()== StatusConstant.DISABLE){
                throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
            }
        }
        //更新套餐状态
        Setmeal setmeal = Setmeal.builder()
                .id(SetmealId)
                .status(status)
                .build();
        setmealMapper.updateById(setmeal);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        //套餐是否停售
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus()== StatusConstant.ENABLE)
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
        }
        //删除套餐
        setmealMapper.deleteByIds(ids);
        //删除套餐和菜品的关联数据
        setmealDishMapper.deleteBySetmealIds(ids);
    }
}