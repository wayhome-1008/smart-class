package com.youlai.boot.building.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 教学楼管理分页查询对象
 *
 * @author way
 * @since 2025-05-08 14:00
 */
@Schema(description ="教学楼管理查询对象")
@Getter
@Setter
public class BuildingQuery extends BasePageQuery {

    @Schema(description = "教学楼编号")
    private String buildingCode;
    @Schema(description = "教学楼名称")
    private String buildingName;
    @Schema(description = "状态")
    private Integer status;
}
