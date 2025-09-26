package com.youlai.boot.device.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备管理分页查询对象
 *
 * @author way
 * @since 2025-05-08 15:16
 */
@Schema(description = "设备管理查询对象")
@Getter
@Setter
public class DeviceQuery extends BasePageQuery {
    @Schema(description = "设备id")
    private Long id;
    @Schema(description = "设备名称")
    private String deviceName;
    @Schema(description = "设备编号")
    private String deviceCode;
    @Schema(description = "教室ids")
    private String roomIds;
    @Schema(description = "部门ids")
    private String deptIds;
    @Schema(description = "分类ids")
    private String categoryIds;
    @Schema(description = "是否锁定 0-未锁定 1-锁定")
    private Integer isLock;
    @Schema(description = "网关")
    private String deviceMac;
    @Schema(description = "是否仅保留存在开关功能的设备 0-不保留 1-保留")
    private Integer isOpen;
    @Schema(description = "设备类型")
    private String deviceTypeIds;
    @Schema(description = "通讯方式")
    private Long communicationModeItemId;
    @Schema(description = "设备序号")
    private String deviceNo;
    @Schema(description = "是否显示网关")
    private Boolean showGateway;
    @Schema(description = "状态")
    private Integer status;
}
