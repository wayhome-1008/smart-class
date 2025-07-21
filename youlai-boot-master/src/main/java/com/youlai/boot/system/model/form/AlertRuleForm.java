package com.youlai.boot.system.model.form;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/**
 * 报警配置表单对象
 *
 * @author way
 * @since 2025-07-21 11:22
 */
@Getter
@Setter
@Schema(description = "报警配置表单对象")
public class AlertRuleForm implements Serializable {

    private Long id;

    @Schema(description = "规则名称")
    private String ruleName;

    @NotNull(message = "不能为空")
    private Long deviceId;

    @Schema(description = "报警级别（1 - 紧急，2 - 重要，3 - 一般，用于后续通知策略）")
//    @NotNull(message = "报警级别（1 - 紧急，2 - 重要，3 - 一般，用于后续通知策略）不能为空")
    private Integer level;

    @Schema(description = "范围下限（当 compare_type 为 “range” 时生效，如 min=10，max=30 表示超出 [10,30] 报警）")
//    @NotNull(message = "范围下限（当 compare_type 为 “range” 时生效，如 min=10，max=30 表示超出 [10,30] 报警）不能为空")
    private Long minThreshold;

    @Schema(description = "范围上限（同上）")
//    @NotNull(message = "范围上限（同上）不能为空")
    private Long maxValue;

    @Schema(description = "对应设备传入属性")
//    @NotBlank(message = "对应设备传入属性不能为空")
    @Size(max = 255, message = "对应设备传入属性长度不能超过255个字符")
    private String metricKey;

    @Schema(description = "比较类型（>、<、>=、<=、==、!=，或 “range” 表示范围）")
//    @NotBlank(message = "比较类型（>、<、>=、<=、==、!=，或 “range” 表示范围）不能为空")
    @Size(max = 255, message = "比较类型（>、<、>=、<=、==、!=，或 “range” 表示范围）长度不能超过255个字符")
    private String compareType;

    @Schema(description = "阈值（如 35，表示 “temperature>35” 报警）")
//    @NotNull(message = "阈值（如 35，表示 “temperature>35” 报警）不能为空")
    private Long thresholdValue;

    @Schema(description = "时间窗口（秒，如 60，表示 “1 分钟内连续 3 次超阈值” 报警）")
//    @NotNull(message = "时间窗口（秒，如 60，表示 “1 分钟内连续 3 次超阈值” 报警）不能为空")
    private Integer timeWindow;

    @Schema(description = "连续次数（如 3，配合 time_window 使用）")
//    @NotNull(message = "连续次数（如 3，配合 time_window 使用）不能为空")
    private Integer consecutiveCount;

    @Schema(description = "备注信息")
    @Size(max = 255, message = "备注信息长度不能超过255个字符")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updateBy;


}
