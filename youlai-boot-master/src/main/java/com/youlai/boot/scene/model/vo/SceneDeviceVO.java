package com.youlai.boot.scene.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 场景设备视图对象
 *
 * @author way
 * @since 2025-07-29 12:03
 */
@Getter
@Setter
@Schema( description = "场景设备视图对象")
public class SceneDeviceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "分类ID")
    private Long categoryId;
    @Schema(description = "分类名称")
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
