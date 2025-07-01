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
 * @since 2025-07-01 09:17
 */
@Getter
@Setter
@Schema( description = "分类管理视图对象")
public class CategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String categoryName;
    @Schema(description = "icon")
    private String icon;
    @Schema(description = "是否禁用(0-启用 1-禁用)")
    private Integer status;
    @Schema(description = "备注信息")
    private String remark;
}
