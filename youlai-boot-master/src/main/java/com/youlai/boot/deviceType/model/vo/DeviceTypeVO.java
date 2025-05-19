package com.youlai.boot.deviceType.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 设备类型字典(自维护)视图对象
 *
 * @author way
 * @since 2025-05-19 10:59
 */
@Getter
@Setter
@Schema( description = "设备类型字典(自维护)视图对象")
public class DeviceTypeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private Long id;
    @Schema(description = "设备类型名称")
    private String deviceType;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
