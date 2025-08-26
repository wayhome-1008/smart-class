package com.youlai.boot.dashBoard.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-08-25  16:18
 *@Description: TODO
 */
@Data
@Schema(description = "房间用电数据")
public class RoomElectricityDataVO {
    @Schema(description = "房间ID")
    private Long roomId;

    @Schema(description = "房间名称")
    private String roomName;

    @Schema(description = "楼宇名称")
    private String buildingName;

    @Schema(description = "楼层名称")
    private String floorName;

    @Schema(description = "房间总用电量")
    private Double totalElectricity;

    @Schema(description = "分类用电量列表")
    private List<CategoryElectricityDataVO> categoryElectricityList;
}
