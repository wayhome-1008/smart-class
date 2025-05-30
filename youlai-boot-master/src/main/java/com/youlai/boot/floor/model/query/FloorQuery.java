package com.youlai.boot.floor.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 楼层管理分页查询对象
 *
 * @author way
 * @since 2025-05-08 12:23
 */
@Schema(description ="楼层管理查询对象")
@Getter
@Setter
public class FloorQuery extends BasePageQuery {

    @Schema(description = "楼层号（如 1、2、-1（负一层））")
    private String floorNumber;

    @Schema(description ="楼宇id")
    private Long buildingId;
}
