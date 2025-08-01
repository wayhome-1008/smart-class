package com.youlai.boot.scene.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/**
 * 规则引擎脚本表单对象
 *
 * @author way
 * @since 2025-07-29 11:54
 */
@Getter
@Setter
@Schema(description = "规则引擎脚本表单对象")
public class RuleScriptForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "不能为空")
    private Long id;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    @Size(max=255, message="分类名称长度不能超过255个字符")
    private String categoryName;

    @Schema(description = "场景ID")
    private Long sceneId;

    @Schema(description = "脚本名称")
    @Size(max=255, message="脚本名称长度不能超过255个字符")
    private String scriptName;

    @Schema(description = "脚本数据")
    @Size(max=255, message="脚本数据长度不能超过255个字符")
    private String scriptData;

    @Schema(description = "应用名称")
    @NotBlank(message = "应用名称不能为空")
    @Size(max=32, message="应用名称长度不能超过32个字符")
    private String applicationName;

    @Schema(description = "脚本类型")
    @Size(max=255, message="脚本类型长度不能超过255个字符")
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
    @Size(max=255, message="脚本语言长度不能超过255个字符")
    private String scriptLanguage;

    @Schema(description = "是否生效")
    private Integer enable;

    @Schema(description = "创建者")
    @Size(max=64, message="创建者长度不能超过64个字符")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    @Size(max=64, message="更新者长度不能超过64个字符")
    private String updateBy;

    @Schema(description = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "备注")
    @Size(max=500, message="备注长度不能超过500个字符")
    private String remark;


}
