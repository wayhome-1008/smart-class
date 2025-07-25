package com.youlai.boot.deviceJob.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务日志视图对象
 *
 * @author way
 * @since 2025-07-25 10:58
 */
@Getter
@Setter
@Schema( description = "任务日志视图对象")
public class DeviceJobLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "设备ID")
    private Long deviceId;
    @Schema(description = "设备名称")
    private String deviceName;
    @Schema(description = "任务名称")
    private String jobName;
    @Schema(description = "任务组名")
    private String jobGroup;
    @Schema(description = "日志信息")
    private String jobMessage;
    @Schema(description = "执行状态（0正常 1失败）")
    private Integer status;
    @Schema(description = "异常信息")
    private String exceptionInfo;
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "停止时间")
    private LocalDateTime stopTime;
}
