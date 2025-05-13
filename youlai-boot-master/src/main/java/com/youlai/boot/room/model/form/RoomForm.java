package com.youlai.boot.room.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/**
 * 房间管理表单对象
 *
 * @author way
 * @since 2025-05-09 12:09
 */
@Getter
@Setter
@Schema(description = "房间管理表单对象")
public class RoomForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private Long id;

    @Schema(description = "所属楼层")
    @NotNull(message = "所属楼层不能为空")
    private Long floorId;

    @Schema(description = "设备")
    @Size(max=255, message="设备长度不能超过255个字符")
    private String deviceId;

    @Schema(description = "所属教学楼")
    @NotNull(message = "所属教学楼不能为空")
    private Long buildingId;

    @Schema(description = "房间号")
    @Size(max=255, message="房间号长度不能超过255个字符")
    private String classroomCode;

    @Schema(description = "备注")
    @Size(max=255, message="备注长度不能超过255个字符")
    private String remark;


}
