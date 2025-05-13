package com.youlai.boot.room.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 房间管理视图对象
 *
 * @author way
 * @since 2025-05-09 12:09
 */
@Getter
@Setter
@Schema( description = "房间管理视图对象")
public class RoomVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private Long id;
    @Schema(description = "所属楼层")
    private Long floorId;
    @Schema(description = "所属教学楼")
    private Long buildingId;
    @Schema(description = "房间号")
    private String classroomCode;
    @Schema(description = "备注")
    private String remark;
}
