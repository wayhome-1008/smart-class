package com.youlai.boot.dashBoard.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-08-11  17:21
 *@Description: TODO
 */
@Data
@Schema(description = "部门分类用电量信息")
public class DepartmentCategoryElectricityInfoVO {

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "总用电量")
    private Double totalElectricity;

    @Schema(description = "该分类总用电量（所有部门中该分类的用电量总和）")
    private Double categoryTotalElectricity;

    @Schema(description = "设备数量")
    private Integer deviceCount;
}