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
        if (deviceInfo != null) {
            List<DeviceInfo> properties = new ArrayList<>();
            //灯光路数
            if (deviceInfo.has("count")) {
                int lightCount = deviceInfo.get("count").asInt();
                properties.add(new DeviceInfo("count", lightCount));
                if (lightCount == 1) {
                    if (deviceInfo.has("POWER")) {
                        properties.add(new DeviceInfo("POWER1", deviceInfo.get("POWER").asText()));
                    }
                } else {
                    for (int i = 1; i <= lightCount; i++) {
                        //灯状态
                        if (deviceInfo.has("POWER" + i)) {
                            properties.add(new DeviceInfo("POWER" + i, deviceInfo.get("POWER" + i).asText()));
                        }
                    }
                }
            }
            return properties;
        }
        return null;
    }
}
