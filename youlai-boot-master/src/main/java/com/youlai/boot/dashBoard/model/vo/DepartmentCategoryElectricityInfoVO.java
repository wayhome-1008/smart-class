package com.youlai.boot.dashBoard.model.vo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-08-11  17:21
 *@Description: TODO
 */
@Data
@Schema(description = "部门分类用电量信息")
@ColumnWidth(20)
public class DepartmentCategoryElectricityInfoVO {

    @ExcelIgnore
    @Schema(description = "部门ID")
    private Long departmentId;

    @ExcelProperty(value = "部门名称")
    @Schema(description = "部门名称")
    private String departmentName;

    @ExcelIgnore
    @Schema(description = "分类ID")
    private Long categoryId;

    @ExcelProperty(value = "分类名称")
    @Schema(description = "分类名称")
    private String categoryName;

    @ExcelProperty(value = "分类用电量")
    @Schema(description = "分类用电量")
    private Double totalElectricity;

    //    @ExcelProperty(value = "部门总用电量")
    @Schema(description = "部门总用电量")
    @ExcelIgnore
    private Double departmentTotalElectricity;

    @ExcelIgnore
    @Schema(description = "设备数量")
    private Integer deviceCount;

    @ExcelIgnore
    @Schema(description = "房间名称")
    private String roomName;

    @ExcelIgnore
    @Schema(description = "房间ID")
    private Long roomId;

    @Schema(description = "房间总用电量")
    private Double roomTotalElectricity;

}