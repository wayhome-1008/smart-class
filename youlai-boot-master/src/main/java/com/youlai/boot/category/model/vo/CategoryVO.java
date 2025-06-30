package com.youlai.boot.category.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 分类管理视图对象
 *
 * @author way
 * @since 2025-06-30 18:52
 */
@Getter
@Setter
@Schema( description = "分类管理视图对象")
public class CategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String categoryName;
    @Schema(description = "是否禁用(0-启用 1-禁用)")
    private Integer status;
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
    @Schema(description = "逻辑删除标识(0-未删除 1-已删除)")
    private Integer isDeleted;
}
