package com.youlai.boot.device.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *@Author: way
 *@CreateTime: 2025-05-07  14:54
 *@Description: mqtt协议：计量插座发送的数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plug {
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
     * 设备的参数信息
     */
    @JSONField(name = "params")
    private PlugParams params;
}
