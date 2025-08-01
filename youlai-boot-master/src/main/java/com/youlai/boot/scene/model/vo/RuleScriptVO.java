package com.youlai.boot.scene.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 规则引擎脚本视图对象
 *
 * @author way
 * @since 2025-07-29 11:54
 */
@Getter
@Setter
@Schema( description = "规则引擎脚本视图对象")
public class RuleScriptVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "分类ID")
    private Long categoryId;
    @Schema(description = "分类名称")
    private String categoryName;
    @Schema(description = "场景ID")
    private Long sceneId;
    @Schema(description = "脚本名称")
    private String scriptName;
    @Schema(description = "脚本数据")
    private String scriptData;
    @Schema(description = "应用名称")
    private String applicationName;
    @Schema(description = "脚本类型")
    private String scriptType;
    @Schema(description = "脚本事件类型")
    private Integer scriptEvent;
    @Schema(description = "脚本动作")
    private Integer scriptAction;
    @Schema(description = "脚本用途")
    private Integer scriptPurpose;
    @Schema(description = "脚本执行顺序")
    private Integer scriptOrder;
    @Schema(description = "脚本语言")
    private String scriptLanguage;
    @Schema(description = "是否生效")
    private Integer enable;
    @Schema(description = "创建者")
    private String createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新者")
    private String updateBy;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "备注")
    private String remark;
}
