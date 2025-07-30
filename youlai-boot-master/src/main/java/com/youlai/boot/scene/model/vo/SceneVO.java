package com.youlai.boot.scene.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 场景交互视图对象
 *
 * @author way
 * @since 2025-07-29 11:51
 */
@Getter
@Setter
@Schema( description = "场景交互视图对象")
public class SceneVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "场景名称")
    private String sceneName;
    @Schema(description = "场景状态")
    private Integer enable;
}
