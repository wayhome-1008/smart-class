package com.youlai.boot.scene.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 执行器视图对象
 *
 * @author way
 * @since 2025-07-30 17:28
 */
@Getter
@Setter
@Schema( description = "执行器视图对象")
public class ActionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sceneId;
    @Schema(description = "动作类型（DEVICE_EXECUTE/PRODUCT_EXECUTE/ALARM_EXECUTE）")
    private String type;
    @Schema(description = "设备ID列表")
    private String deviceIds;
    @Schema(description = "分类id")
    private Long categoryId;
    @Schema(description = "报警配置id")
    private Long alertRuleId;
    private String parameters;
}
