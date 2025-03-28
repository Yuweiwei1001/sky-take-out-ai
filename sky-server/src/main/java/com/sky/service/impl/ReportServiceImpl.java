package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2025/3/24 11:26
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * @description 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return com.sky.vo.TurnoverReportVO
     * @author yw
     * @date 2025/3/24 11:26
     */
    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        // 先将日期一天一天加入到list，再将list转为字符串，逗号分隔
        List<LocalDate> dateList = new ArrayList<>();
        // 遍历日期
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        //将list转为字符串，逗号分隔
        String dateListStr = StringUtils.join(dateList, ",");
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 获取当日营业额
            // 查询当日开始时间2025年3月24日0点0分0秒和结束时间23点59分59.999...秒
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Double turnover = orderMapper.getTurnoverSumByDate(beginTime, endTime, Orders.COMPLETED);
            turnover = turnover == null ? 0.0 : turnover; // 如果turnover为null，则设置为0.0
            // 将营业额加入到turnoverList
            turnoverList.add(turnover);
        }
        String turnoverListStr = StringUtils.join(turnoverList, ",");
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        // 遍历日期
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        //将list转为字符串，逗号分隔
        String dateListStr = StringUtils.join(dateList, ",");

        //统计用户总量列表
        List<Integer> totalUserList = new ArrayList<>();
        // 统计新增用户列表
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 获取当日用户总量
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer totalUser = userMapper.countByDate(null,endTime);
            totalUser = totalUser == null ? 0 : totalUser;
            totalUserList.add(totalUser);

            // 获取当日新增用户量
            Integer newUser = userMapper.countByDate(beginTime,endTime);
            newUser = newUser == null ? 0 : newUser;
            newUserList.add(newUser);
        }
        String totalUserListStr = StringUtils.join(totalUserList, ",");
        String newUserListStr = StringUtils.join(newUserList, ",");
        return UserReportVO.builder()
                .dateList(dateListStr)
                .totalUserList(totalUserListStr)
                .newUserList(newUserListStr)
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        String dateListStr = StringUtils.join(dateList, ",");
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        List<Double> orderCompletionRateList = new ArrayList<>();

        // 遍历日期, 获取当日订单总数量，有效订单数量，订单完成率
        for (LocalDate date : dateList) {
            // 获取当日订单总数量
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = orderMapper.countByDate(beginTime, endTime,null);
            orderCount = orderCount == null ? 0 : orderCount;
            orderCountList.add(orderCount);

            // 获取当日有效订单数量(已完成的订单)
            Integer validOrderCount = orderMapper.countByDate(beginTime, endTime, Orders.COMPLETED);
            validOrderCount = validOrderCount == null ? 0 : validOrderCount;
            validOrderCountList.add(validOrderCount);
        }

        // 计算时间区间内的订单总数、有效订单总数和订单完成率
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        Double orderCompletionRate = 0.0;
        if (totalOrderCount > 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();
        }

        String orderCountListStr = StringUtils.join(orderCountList, ",");
        String validOrderCountListStr = StringUtils.join(validOrderCountList, ",");


        return OrderReportVO.builder()
                .dateList(dateListStr)
                .orderCountList(orderCountListStr)
                .validOrderCountList(validOrderCountListStr)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计销售top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {

        // 查询top10
        // select od.name, sum(od.number) as number from orders o, order_detail od where o.id = od.order_id and o.status = 5 and o.order_time between '2025-03-24 00:00:00' and '2025-03-24 23:59:59.999999'
        // group by od.name order by number desc limit 10;
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);


        //stream流处理
        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String nameListStr = StringUtils.join(nameList, ",");
        String numberListStr = StringUtils.join(numberList, ",");


        return SalesTop10ReportVO
                .builder()
                .numberList(numberListStr)
                .nameList(nameListStr)
                .build();
    }

    /**
     * 导出报表数据
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1.查询数据库，获取营业数据
        LocalDate beginDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDateTime beginTime = LocalDateTime.of(beginDate, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.MAX);
        BusinessDataVO businessData = workspaceService.getBusinessData(beginTime, endTime);
        // 2.将数据写入excel
        // 读取模板文件
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/business-data.xlsx");

        try {
            //基于模板文件创建一个新的excel文件
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);

            //获取sheet
            XSSFSheet sheet = excel.getSheet("Sheet1");

            // 获取行
            XSSFRow row1 = sheet.getRow(1);
            // 填充数据 - 时间
            row1.getCell(1).setCellValue("时间" + beginDate + "至" + endDate);
            XSSFRow row3 = sheet.getRow(3);
            // 填充数据 - 营业额
            row3.getCell(2).setCellValue(businessData.getTurnover());
            // 填充数据 - 订单完成率
            row3.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            // 填充数据 - 新增用户数
            row3.getCell(6).setCellValue(businessData.getNewUsers());
            XSSFRow row4 = sheet.getRow(4);
            // 填充数据 - 有效订单
            row4.getCell(2).setCellValue(businessData.getValidOrderCount());
            // 填充数据 - 平均客单价
            row4.getCell(4).setCellValue(businessData.getUnitPrice());

            // 填充明细数据
           for (int i = 0; i < 30 ; i++){
                LocalDate date = beginDate.plusDays(i);
                //查询某一天的营业额
                BusinessDataVO businessDataOneDay = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获取某一行
                // 填充数据 - 明细日期
                XSSFRow row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                // 填充数据 - 营业额
                row.getCell(2).setCellValue(businessDataOneDay.getTurnover());
                // 填充数据 - 有效订单
                row.getCell(3).setCellValue(businessDataOneDay.getValidOrderCount());
                // 填充数据 - 订单完成率
                row.getCell(4).setCellValue(businessDataOneDay.getOrderCompletionRate());
                // 填充数据 - 平均客单价
                row.getCell(5).setCellValue(businessDataOneDay.getUnitPrice());
                // 填充数据 - 新增用户数
                row.getCell(6).setCellValue(businessDataOneDay.getNewUsers());
           }

            // 3.通过输出流，将excel下载到客户端浏览器
            OutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException("导出报表失败");
        }

    }
}