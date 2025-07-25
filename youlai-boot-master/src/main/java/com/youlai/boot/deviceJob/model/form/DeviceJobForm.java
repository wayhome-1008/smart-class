package com.youlai.boot.deviceJob.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务管理表单对象
 *
 * @author way
 * @since 2025-06-30 18:27
 */
@Getter
@Setter
@Schema(description = "任务管理表单对象")
public class DeviceJobForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "不能为空")
    private Long id;

    @Schema(description = "定时任务设备")
    @NotNull(message = "定时任务设备不能为空")
    private Long deviceId;

    private String deviceName;

    /** 执行动作 */
    private String actions;

    /** 是否并发执行（0允许 1禁止） */
    private Integer concurrent;

    private String jobName; // 任务名称，唯一标识一个任务

    private String jobGroup; // 任务组名，用于对任务进行分组管理


    @Schema(description = "1=设备定时，2=设备告警，3=场景联动")
    @NotNull(message = "任务类型不能为空")
    private Long typeId;

    @Schema(description = "cron表达式")
    @NotBlank(message = "cron表达式不能为空")
    @Size(max = 255, message = "cron表达式长度不能超过255个字符")
    private String cron;

    @Schema(description = "0:暂停 1:运行")
    @NotNull(message = "0:暂停 1:运行不能为空")
    private Integer status;

    @Schema(description = "备注信息")
    @Size(max = 255, message = "备注信息长度不能超过255个字符")
    private String remark;


}
