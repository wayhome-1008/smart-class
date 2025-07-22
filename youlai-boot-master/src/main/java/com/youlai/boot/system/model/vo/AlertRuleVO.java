package com.youlai.boot.system.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 报警配置视图对象
 *
 * @author way
 * @since 2025-07-21 11:22
 */
@Getter
@Setter
@Schema(description = "报警配置视图对象")
public class AlertRuleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "规则名称")
    private String ruleName;

    private Long id;
    @Schema(description = "报警级别（1 - 紧急，2 - 重要，3 - 一般，用于后续通知策略）")
    private Integer level;
    @Schema(description = "范围下限（当 compare_type 为 “range” 时生效，如 min=10，max=30 表示超出 [10,30] 报警）")
    private Long minValue;
    @Schema(description = "范围上限（同上）")
    private Long maxValue;
    @Schema(description = "对应设备传入属性")
    private String metricKey;
    @Schema(description = "比较类型（>、<、>=、<=、==、!=，或 “range” 表示范围）")
    private String compareType;
    @Schema(description = "阈值（如 35，表示 “temperature>35” 报警）")
    private Long thresholdValue;
    @Schema(description = "时间窗口（秒，如 60，表示 “1 分钟内连续 3 次超阈值” 报警）")
    private Integer timeWindow;
    @Schema(description = "连续次数（如 3，配合 time_window 使用）")
    private Integer consecutiveCount;
    @Schema(description = "备注信息")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "创建人ID")
    private Long createBy;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "更新人ID")
    private Long updateBy;
}
