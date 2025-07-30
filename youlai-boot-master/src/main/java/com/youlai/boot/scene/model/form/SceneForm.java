package com.youlai.boot.scene.model.form;

import java.io.Serial;
import java.io.Serializable;

import com.youlai.boot.scene.model.entity.SceneScript;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/**
 * 场景交互表单对象
 *
 * @author way
 * @since 2025-07-29 11:51
 */
@Getter
@Setter
@Schema(description = "场景交互表单对象")
public class SceneForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "场景名称")
    @Size(max=255, message="场景名称长度不能超过255个字符")
    private String sceneName;

    @Schema(description = "执行条件")
    private Integer cond;

    @Schema(description = "静默周期(分钟)")
    private Integer silentPeriod;

    @Schema(description = "执行方式")
    private Integer executeMode;

    @Schema(description = "延时执行(秒钟)")
    private Integer executeDelay;

    @Schema(description = "是否包含告警推送")
    private Integer hasAlert;

    @Schema(description = "场景状态")
    private Integer enable;

    @Schema(description = "备注")
    @Size(max=500, message="备注长度不能超过500个字符")
    private String remark;

    /** 接收的触发器列表 */
    private List<SceneScript> triggers;

    /** 接收的执行动作列表 */
    private List<SceneScript> actions;

}
