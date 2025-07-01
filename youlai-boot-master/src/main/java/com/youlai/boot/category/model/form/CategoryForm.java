package com.youlai.boot.category.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/**
 * 分类管理表单对象
 *
 * @author way
 * @since 2025-07-01 09:17
 */
@Getter
@Setter
@Schema(description = "分类管理表单对象")
public class CategoryForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "不能为空")
    private Long id;

    @NotBlank(message = "不能为空")
    @Size(max=255, message="长度不能超过255个字符")
    private String categoryName;

    @Schema(description = "icon")
    @NotBlank(message = "icon不能为空")
    @Size(max=255, message="icon长度不能超过255个字符")
    private String icon;

    @Schema(description = "是否禁用(0-启用 1-禁用)")
    @NotNull(message = "是否禁用(0-启用 1-禁用)不能为空")
    private Integer status;

    @Schema(description = "备注信息")
    @Size(max=255, message="备注信息长度不能超过255个字符")
    private String remark;


}
