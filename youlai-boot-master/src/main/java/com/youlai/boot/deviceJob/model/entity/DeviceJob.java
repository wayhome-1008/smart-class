package com.youlai.boot.deviceJob.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务管理实体对象
 *
 * @author way
 * @since 2025-06-30 18:27
 */
@Getter
@Setter
@TableName("device_job")
public class DeviceJob extends BaseEntity {

    /** 设备id */
    private Long deviceId;

    /** 场景联动ID */
    private Long sceneId;

    /** 设备名称 */
    private String deviceName;

    /** 执行动作 */
    private String actions;

    /** 是否并发执行（0允许 1禁止） */
    private Integer concurrent;

    /** 任务名称 */
    private String jobName;

    /** 任务组名 */
    private String jobGroup;


    /** 定时类型（1=设备定时，2=设备告警，3=场景联动） */
    private Long jobType;

    /** cron执行表达式 */
    private String cron;

    /** 是否详细corn表达式 1：是，0：否 */
    private Integer isAdvance;

    /** 任务状态（0正常 1暂停） */
    private Integer status;
    /**
     * 备注信息
     */
    private String remark;
    /**
     * 创建人ID
     */
    private Long createBy;
    /**
     * 更新人ID
     */
    private Long updateBy;
}
