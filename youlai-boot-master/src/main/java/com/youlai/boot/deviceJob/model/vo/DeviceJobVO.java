package com.youlai.boot.deviceJob.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 任务管理视图对象
 *
 * @author way
 * @since 2025-06-30 18:11
 */
@Getter
@Setter
@Schema( description = "任务管理视图对象")
public class DeviceJobVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "定时任务设备")
    private Long deviceId;
    @Schema(description = "任务类型")
    private Long typeId;
    @Schema(description = "cron表达式")
    private String cron;
    @Schema(description = "0:暂停 1:运行")
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
