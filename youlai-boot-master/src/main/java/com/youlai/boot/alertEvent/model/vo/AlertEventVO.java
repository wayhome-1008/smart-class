package com.youlai.boot.alertEvent.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 报警记录视图对象
 *
 * @author way
 * @since 2025-07-21 12:16
 */
@Getter
@Setter
@Schema( description = "报警记录视图对象")
public class AlertEventVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "关联的规则 ID")
    private Long ruleId;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "创建人ID")
    private Long createBy;
    @Schema(description = "触发报警的设备 ID")
    private Long deviceId;
    @Schema(description = "触发报警的设备名称")
    private String deviceName;
    @Schema(description = "触发的指标")
    private String metricKey;
    @Schema(description = "当前值（如 36.5℃）")
    private Long currentValue;
    @Schema(description = "报警内容（如 “温度 36.5℃，超出阈值 35℃”）")
    private String alarmContent;
    @Schema(description = "报警级别（继承规则的 level）")
    private Integer level;
    @Schema(description = "事件状态（0 - 未处理，1 - 已处理，2 - 已忽略）")
    private String status;
}
