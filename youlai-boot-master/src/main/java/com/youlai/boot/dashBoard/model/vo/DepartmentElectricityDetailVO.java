package com.youlai.boot.dashBoard.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-08-25  16:17
 *@Description: TODO
 */
@Data
public class DepartmentElectricityDetailVO {
    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "部门总用电量")
    private Double totalElectricity;

    @Schema(description = "房间用电量列表")
    private List<RoomElectricityDataVO> roomElectricityList;
}
