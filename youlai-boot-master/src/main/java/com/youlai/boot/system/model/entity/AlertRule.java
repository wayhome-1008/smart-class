package com.youlai.boot.system.model.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

/**
 * 报警配置实体对象
 *
 * @author way
 * @since 2025-07-21 11:22
 */
@Getter
@Setter
@TableName("alert_rule")
public class AlertRule extends BaseEntity {

    private String ruleName;

    private Long deviceId;
    /**
     * 报警级别（1 - 紧急，2 - 重要，3 - 一般，用于后续通知策略）
     */
    private Integer level;
    /**
     * 范围下限（当 compare_type 为 “range” 时生效，如 min=10，max=30 表示超出 [10,30] 报警）
     */
    private Long minValue;
    /**
     * 范围上限（同上）
     */
    private Long maxValue;
    /**
     * 对应设备传入属性
     */
    private String metricKey;
    /**
     * 比较类型（>、<、>=、<=、==、!=，或 “range” 表示范围）
     */
    private String compareType;
    /**
     * 阈值（如 35，表示 “temperature>35” 报警）
     */
    private Long thresholdValue;
    /**
     * 时间窗口（秒，如 60，表示 “1 分钟内连续 3 次超阈值” 报警）
     */
    private Integer timeWindow;
    /**
     * 连续次数（如 3，配合 time_window 使用）
     */
    private Integer consecutiveCount;
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
