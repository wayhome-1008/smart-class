package com.youlai.boot.scene.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

/**
 * 执行器表单对象
 *
 * @author way
 * @since 2025-07-30 17:28
 */
@Getter
@Setter
@Schema(description = "执行器表单对象")
public class ActionForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "不能为空")
    private Long id;

    private Long sceneId;

    @Schema(description = "动作类型（DEVICE_EXECUTE/PRODUCT_EXECUTE/ALARM_EXECUTE）")
    @Size(max=255, message="动作类型（DEVICE_EXECUTE/PRODUCT_EXECUTE/ALARM_EXECUTE）长度不能超过255个字符")
    private String type;

    @Schema(description = "设备ID列表")
    @Size(max=255, message="设备ID列表长度不能超过255个字符")
    private String deviceIds;

    @Schema(description = "分类id")
    private Long categoryId;

    @Schema(description = "报警配置id")
    private Long alertRuleId;

    @Size(max=65535, message="长度不能超过65535个字符")
    private String parameters;


}
