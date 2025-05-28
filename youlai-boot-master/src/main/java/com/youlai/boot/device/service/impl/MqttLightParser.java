package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  17:05
 *@Description: MQTT协议灯光设备解析
 */
public class MqttLightParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        List<DeviceInfo> properties = new ArrayList<>();
        //灯状态
        if (deviceInfo.has("POWER")) {
            properties.add(new DeviceInfo("power", deviceInfo.get("POWER").asText()));
        }
        return properties;
    }
}
