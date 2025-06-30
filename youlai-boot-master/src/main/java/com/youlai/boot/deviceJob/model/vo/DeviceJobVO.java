package com.youlai.boot.deviceJob.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 任务管理视图对象
 *
 * @author way
 * @since 2025-06-30 18:27
 */
@Getter
@Setter
@Schema( description = "任务管理视图对象")
public class DeviceJobVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "定时任务设备")
    private Long deviceId;
    @Schema(description = "任务类型")
    private Long typeId;
    @Schema(description = "cron表达式")
    private String cron;
    @Schema(description = "0:暂停 1:运行")
    private Integer status;
    @Schema(description = "备注信息")
    private String remark;
}
