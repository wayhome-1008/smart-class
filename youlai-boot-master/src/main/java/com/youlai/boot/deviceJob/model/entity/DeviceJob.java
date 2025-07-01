package com.youlai.boot.deviceJob.model.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

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

    /**
     * 定时任务设备
     */
    private Long deviceId;

    private String jobName; // 任务名称，唯一标识一个任务

    private String jobGroup; // 任务组名，用于对任务进行分组管理

    private String jobClass; // 任务执行类，指定任务具体执行逻辑的类名

    private Boolean enable;
    /**
     * 任务类型
     */
    private Long typeId;
    /**
     * cron表达式
     */
    private String cron;
    /**
     * 0:暂停 1:运行
     */
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
    /**
     * 逻辑删除标识(0-未删除 1-已删除)
     */
    private Integer isDeleted;
}
