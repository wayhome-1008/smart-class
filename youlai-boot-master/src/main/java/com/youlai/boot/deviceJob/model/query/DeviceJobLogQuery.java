package com.youlai.boot.deviceJob.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志分页查询对象
 *
 * @author way
 * @since 2025-07-25 10:58
 */
@Schema(description ="任务日志查询对象")
@Getter
@Setter
public class DeviceJobLogQuery extends BasePageQuery {

    @Schema(description = "设备名称")
    private String deviceName;
    @Schema(description = "任务名称")
    private String jobName;
    @Schema(description = "执行状态（0正常 1失败）")
    private Integer status;
}
