package com.youlai.boot.device.model.vo;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  10:16
 *@Description: TODO
 */
@Data
public class DeviceInfoVO {
    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 设备编号
     */
    private String deviceCode;
    /**
     * 教室
     */
    private Long deviceRoom;
    private String roomName;
    /**
     * 网关
     */
    private String deviceMac;
    /**
     * 网关子设备绑定网关主键id
     */
    private Long deviceGatewayId;
    /**
     * 设备类型
     */
    private Long deviceTypeId;
    private Long communicationModeItemId;
    private String deviceNo;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 备注信息
     */
    private String remark;

    private List<DeviceInfo> deviceInfo;
}
