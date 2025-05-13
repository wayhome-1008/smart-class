package com.youlai.boot.floor.model.form;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 楼层管理表单对象
 *
 * @author way
 * @since 2025-05-08 12:23
 */
@Getter
@Setter
@Schema(description = "楼层管理表单对象")
public class FloorForm{

    private Long id;

    @Schema(description = "所属教学楼 ID（外键）")
    @NotNull(message = "所属教学楼 ID（外键）不能为空")
    private Long buildingId;

    @Schema(description = "楼层号（如 1、2、-1（负一层））")
    private String floorNumber;

    @Schema(description = "备注")
    @Size(max=255, message="备注长度不能超过255个字符")
    private String remark;

//    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime createTime;
//
//    private Long createBy;
//
//    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime updateTime;
//
//    private Long updateBy;

//    @NotNull(message = "不能为空")
//    private Integer isDeleted;


}
