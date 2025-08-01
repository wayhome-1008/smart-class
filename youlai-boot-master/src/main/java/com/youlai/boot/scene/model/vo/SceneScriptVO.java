package com.youlai.boot.scene.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 场景脚本视图对象
 *
 * @author way
 * @since 2025-07-29 11:58
 */
@Getter
@Setter
@Schema( description = "场景脚本视图对象")
public class SceneScriptVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "场景ID")
    private Long sceneId;
    @Schema(description = "任务ID")
    private Long jobId;
    @Schema(description = "触发源")
    private Integer source;
    @Schema(description = "脚本用途")
    private Integer scriptPurpose;
    @Schema(description = "操作符")
    private String operator;
    @Schema(description = "功能类别")
    private Integer type;
    @Schema(description = "设备数量")
    private Integer deviceCount;
    @Schema(description = "cron表达式")
    private String cron;
    @Schema(description = "是否自定义表达式，1：是，0：否")
    private Integer isAdvance;
    @Schema(description = "数组索引")
    private String arrayIndex;
    @Schema(description = "数组索引名称")
    private String arrayIndexName;
    @Schema(description = "分类ID")
    private Long categoryId;
    @Schema(description = "分类名称")
    private String categoryName;
    @Schema(description = "备注信息")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "创建人ID")
    private Long createBy;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "更新人ID")
    private Long updateBy;
}
