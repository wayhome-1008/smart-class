package com.youlai.boot.dashBoard.model.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-08-26  17:53
 *@Description: TODO
 */
@Data
public class ExportDepartment {
    @ExcelProperty(value = "部门id")
    private Long departmentId;
    @ExcelProperty(value = "部门名称")
    private String departmentName;
    @ExcelProperty(value = "用电量")
    private Double totalElectricity;
    @ExcelProperty(value = "分类名称")
    private String categoryName;


}
