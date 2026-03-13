package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.service.TokenService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags = "员工相关接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("员工登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        // 登录成功后，生成双token
        // 1. 生成 Access Token（短期有效）
        String accessToken = tokenService.generateAccessToken(employee.getId());

        // 2. 生成 Refresh Token（长期有效，存储在Redis中）
        String refreshToken = tokenService.generateRefreshToken(employee.getId());

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAdminTtl() / 1000) // 转换为秒
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     * 吊销当前用户的所有 Refresh Token
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("员工退出")
    public Result<String> logout() {
        // 从 BaseContext 获取当前登录员工ID，吊销其所有 Refresh Token
        Long empId = com.sky.context.BaseContext.getCurrentId();
        if (empId != null) {
            tokenService.revokeRefreshToken(empId);
            log.info("员工退出，吊销所有Token，员工ID：{}", empId);
        }
        return Result.success();
    }


    @PostMapping
    @ApiOperation("新增员工")
    public Result<String> save(@RequestBody EmployeeDTO employeeDTO){
        employeeService.save(employeeDTO);
        return Result.success("新增员工成功");
    }

    @GetMapping("/page")
    @ApiOperation("分页查询员工信息")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO){
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用员工账号")
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        log.info("启用禁用员工账号：{},{}",status,id);
        employeeService.startOrStop(status,id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询员工信息")
    public Result<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息：{}",id);
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }

    @PutMapping
    @ApiOperation("修改员工信息")
    public Result update(@RequestBody EmployeeDTO employeeDTO){
        log.info("员工信息修改：{}",employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }
}
