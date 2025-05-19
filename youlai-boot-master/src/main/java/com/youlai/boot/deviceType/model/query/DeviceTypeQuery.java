package com.youlai.boot.deviceType.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备类型字典(自维护)分页查询对象
 *
 * @author way
 * @since 2025-05-19 10:59
 */
@Schema(description ="设备类型字典(自维护)查询对象")
@Getter
@Setter
public class DeviceTypeQuery extends BasePageQuery {

    @Schema(description = "设备类型名称")
    private String deviceType;
}
