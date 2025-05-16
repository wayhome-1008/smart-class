package com.youlai.boot.device.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-16  12:44
 *@Description: TODO
 */
@Data
public class HumanRadarSensor {
    /**
     * 消息序列号
     */
    @JSONField(name = "sequence")
    private Integer sequence;

    /**
     * 设备识别号，网关向服务器注册子设备时，服务器分配的唯一 ID
     */
    @JSONField(name = "deviceId")
    private String deviceId;

    @JSONField(name = "params")
    private HumanRadarSensorParams params;
}
