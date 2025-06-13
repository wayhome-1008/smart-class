package com.youlai.boot.room.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房间管理分页查询对象
 *
 * @author way
 * @since 2025-05-09 12:09
 */
@Schema(description ="房间管理查询对象")
@Getter
@Setter
public class RoomQuery extends BasePageQuery {
    @Schema(description = "楼宇id")
    private Long buildingId;

    @Schema(description = "楼层id")
    private Long floorId;

    @Schema(description = "房间号")
    private String classroomCode;


}
