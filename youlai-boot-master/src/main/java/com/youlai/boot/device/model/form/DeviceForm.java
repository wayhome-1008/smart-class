package com.youlai.boot.device.model.form;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.config.handler.JsonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 设备管理表单对象
 *
 * @author way
 * @since 2025-05-08 15:16
 */
@Getter
@Setter
@Schema(description = "设备管理表单对象")
public class DeviceForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备编号")
    private String deviceCode;

    @Schema(description = "教室")
    private Long deviceRoom;

    @Schema(description = "网关")

    private String deviceMac;

    private Long deviceGatewayId;

    private Long categoryId;
    @Schema(description = "设备类型")
    private Long deviceTypeId;
    @Schema(description = "设备类型名称")
    private String deviceType;
    @Schema(description = "通讯方式")
    private Long communicationModeItemId;
    @Schema(description = "通讯方式名称")
    private String communicationModeItemName;
    @Schema(description = "设备序号")
    private String deviceNo;
    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注信息")
    private String remark;

    @TableField(
            value = "device_info",
            typeHandler = JsonTypeHandler.class  // 自定义TypeHandler
    )
    private JsonNode deviceInfo;  // 存储JSON数据
}
