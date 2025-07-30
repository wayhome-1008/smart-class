package com.youlai.boot.scene.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

/**
 * 触发器表单对象
 *
 * @author way
 * @since 2025-07-30 17:25
 */
@Getter
@Setter
@Schema(description = "触发器表单对象")
public class TriggerForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "不能为空")
    private Long id;

    private Long sceneId;

    @Schema(description = "触发类型（DEVICE_TRIGGER/PRODUCT_TRIGGER/TIMER_TRIGGER）")
    @Size(max=255, message="触发类型（DEVICE_TRIGGER/PRODUCT_TRIGGER/TIMER_TRIGGER）长度不能超过255个字符")
    private String type;

    @Schema(description = "设备ID列表")
    @Size(max=255, message="设备ID列表长度不能超过255个字符")
    private String deviceIds;

    @Schema(description = "分类id")
    private Long categoryId;

    @Schema(description = "Cron表达式")
    @NotBlank(message = "Cron表达式不能为空")
    @Size(max=255, message="Cron表达式长度不能超过255个字符")
    private String cron;


    private String threshold;

    @Schema(description = "阈值条件逻辑")
    @Size(max=255, message="阈值条件逻辑长度不能超过255个字符")
    private String thresholdLogic;


}
