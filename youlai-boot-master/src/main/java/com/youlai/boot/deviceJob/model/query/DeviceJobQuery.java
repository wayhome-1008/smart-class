package com.youlai.boot.deviceJob.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务管理分页查询对象
 *
 * @author way
 * @since 2025-06-30 18:11
 */
@Schema(description ="任务管理查询对象")
@Getter
@Setter
public class DeviceJobQuery extends BasePageQuery {

    @Schema(description = "任务类型")
    private Long typeId;
    @Schema(description = "0:暂停 1:运行")
    private Integer status;
}
