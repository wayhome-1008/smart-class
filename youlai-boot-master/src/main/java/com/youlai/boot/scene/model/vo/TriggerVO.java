package com.youlai.boot.scene.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 触发器视图对象
 *
 * @author way
 * @since 2025-07-30 17:25
 */
@Getter
@Setter
@Schema( description = "触发器视图对象")
public class TriggerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sceneId;
    @Schema(description = "触发类型（DEVICE_TRIGGER/PRODUCT_TRIGGER/TIMER_TRIGGER）")
    private String type;
    @Schema(description = "设备ID列表")
    private String deviceIds;
    @Schema(description = "分类id")
    private Long categoryId;
    @Schema(description = "Cron表达式")
    private String cron;
    @Schema(description = "触发阈值条件（")
    private String threshold;
    @Schema(description = "阈值条件逻辑")
    private String thresholdLogic;
}
