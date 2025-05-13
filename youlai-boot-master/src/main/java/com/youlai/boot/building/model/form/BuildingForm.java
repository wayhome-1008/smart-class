package com.youlai.boot.building.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/**
 * 教学楼管理表单对象
 *
 * @author way
 * @since 2025-05-08 14:00
 */
@Getter
@Setter
@Schema(description = "教学楼管理表单对象")
public class BuildingForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @Schema(description = "教学楼编号")
    @NotBlank(message = "教学楼编号不能为空")
    @Size(max=255, message="教学楼编号长度不能超过255个字符")
    private String buildingCode;

    @Schema(description = "教学楼名称")
    @NotBlank(message = "教学楼名称不能为空")
    @Size(max=255, message="教学楼名称长度不能超过255个字符")
    private String buildingName;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注信息")
    @Size(max=255, message="备注信息长度不能超过255个字符")
    private String remark;


}
