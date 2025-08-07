package com.youlai.boot.scene.model.form;

import java.io.Serializable;

import com.youlai.boot.scene.model.entity.Action;
import com.youlai.boot.scene.model.entity.Trigger;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
    @Schema(description = "场景名称")
    @Size(max = 255, message = "场景名称长度不能超过255个字符")
    private String sceneName;

    @Schema(description = "执行条件 ALL：所有  ANY：任意 NOT：不满足")
    private String conditionType;

    @Schema(description = "静默周期(分钟)")
    private Integer silentTime;

    @Schema(description = "执行方式 SERIAL:串行 PARALLEL：并行")
    private String executeMode;

    @Schema(description = "延时执行(秒钟)")
    private Integer delaySeconds;

    @Schema(description = "场景状态  1启用 0禁用")
    private Integer enable;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /** 接收的触发器列表 */
    private List<Trigger> triggers;

    /** 接收的执行动作列表 */
    private List<Action> actions;

}
