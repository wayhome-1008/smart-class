package com.youlai.boot.building.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 教学楼管理视图对象
 *
 * @author way
 * @since 2025-05-08 14:00
 */
@Getter
@Setter
@Schema(description = "教学楼管理视图对象")
public class BuildingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "教学楼编号")
    private String buildingCode;
    @Schema(description = "教学楼名称")
    private String buildingName;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "备注信息")
    private String remark;
    @Schema(description = "楼层数量")
    private Integer floorCount;
    @Schema(description = "房间数量")
    private Integer roomCount;
    @Schema(description = "设备数量")
    private Integer deviceCount;


}
