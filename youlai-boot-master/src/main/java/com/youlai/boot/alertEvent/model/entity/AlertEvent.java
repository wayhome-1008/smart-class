package com.youlai.boot.alertEvent.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 报警记录实体对象
 *
 * @author way
 * @since 2025-07-21 12:16
 */
@Getter
@Setter
@TableName("alert_event")
public class AlertEvent extends BaseEntity {

    /**
     * 关联的规则 ID
     */
    private Long ruleId;

    /**
     * 触发报警的设备 ID
     */
    private Long deviceId;
    /**
     * 触发的指标
     */
    private String metricKey;
    /**
     * 当前值（如 36.5℃）
     */
    private Long currentValue;
    /**
     * 报警内容（如 “温度 36.5℃，超出阈值 35℃”）
     */
    private String alarmContent;
    /**
     * 报警级别（继承规则的 level）
     */
    private Integer level;
    /**
     * 事件状态（0 - 未处理，1 - 已处理，2 - 已忽略）
     */
    private String status;

    @TableField(exist = false)
    private Long eventTime;
}
