package com.youlai.boot.scene.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

/**
 * 场景设备表单对象
 *
 * @author way
 * @since 2025-07-29 12:03
 */
@Getter
@Setter
@Schema(description = "场景设备表单对象")
public class SceneDeviceForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "不能为空")
    private Long id;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    @Size(max=255, message="分类名称长度不能超过255个字符")
    private String categoryName;

    @Schema(description = "场景脚本ID")
    private Long scriptId;

    @Schema(description = "场景ID")
    private Long sceneId;

    @Schema(description = "功能类别")
    private Integer type;

    @Schema(description = "触发源")
    private Integer source;


}
