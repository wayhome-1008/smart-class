package com.youlai.boot.dashBoard.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-08-15  11:55
 *@Description: TODO
 */
@Data
@Schema(description = "部门用电量信息")
public class DepartmentElectricityVO {

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "总用电量")
    private Double totalElectricity;
}