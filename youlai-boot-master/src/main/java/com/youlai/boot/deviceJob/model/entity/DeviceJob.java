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
 * @since 2025-06-30 18:11
 */
@Getter
@Setter
@TableName("device_job")
public class DeviceJob extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 定时任务设备
     */
    private Long deviceId;
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
