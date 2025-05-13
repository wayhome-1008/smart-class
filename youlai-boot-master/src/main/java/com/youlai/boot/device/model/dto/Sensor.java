package com.youlai.boot.device.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-07  11:30
 *@Description: mqtt协议：传感器发送的json完整版
 */
@Data
public class Sensor {
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

    /**
     * 设备参数信息
     */
    @JSONField(name = "params")
    private SensorParams params;
}
