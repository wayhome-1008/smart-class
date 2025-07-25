package com.youlai.boot.deviceJob.model.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

/**
 * 任务日志实体对象
 *
 * @author way
 * @since 2025-07-25 10:58
 */
@Getter
@Setter
@TableName("device_job_log")
public class DeviceJobLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    private Long deviceId;
    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 任务名称
     */
    private String jobName;
    /**
     * 任务组名
     */
    private String jobGroup;
    /**
     * 日志信息
     */
    private String jobMessage;
    /**
     * 执行状态（0正常 1失败）
     */
    private Integer status;
    /**
     * 异常信息
     */
    private String exceptionInfo;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 停止时间
     */
    private LocalDateTime stopTime;
}
