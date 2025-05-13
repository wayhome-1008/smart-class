package com.youlai.boot.device.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 设备管理视图对象
 *
 * @author way
 * @since 2025-05-08 15:16
 */
@Getter
@Setter
@Schema( description = "设备管理视图对象")
public class DeviceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "设备名称")
    private String deviceName;
    @Schema(description = "设备编号")
    private String deviceCode;
    @Schema(description = "教室")
    private Long deviceRoom;
    @Schema(description = "网关")
    private String deviceMac;
    @Schema(description = "设备类型")
    private Long deviceTypeItemId;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "备注信息")
    private String remark;
    //根据设备存储的字典id查询出名称
    private String label;
    //房间名称
    private String roomName;
}
