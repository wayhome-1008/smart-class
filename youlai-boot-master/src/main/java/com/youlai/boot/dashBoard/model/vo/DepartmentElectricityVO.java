package com.youlai.boot.dashBoard.model.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-08-15  11:55
 *@Description: TODO
 */
@Data
@Schema(description = "部门用电量信息")
@ColumnWidth(20)
public class DepartmentElectricityVO {
    @ExcelProperty(value = "部门号")
    @Schema(description = "部门ID")
    private Long departmentId;

    @ExcelProperty(value = "部门名称")
    @Schema(description = "部门名称")
    private String departmentName;

    @ExcelProperty(value = "总用电量")
    @Schema(description = "总用电量")
    private Double totalElectricity;

    @ExcelProperty(value = "分类名称")
    @Schema(description = "分类名称")
    private String categoryName;
}