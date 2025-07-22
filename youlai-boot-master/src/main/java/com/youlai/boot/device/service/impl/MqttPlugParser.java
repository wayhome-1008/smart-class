package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-06-20  10:02
 *@Description: MQTT计量插座解析器
 */
public class MqttPlugParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        if (deviceInfo != null) {
            List<DeviceInfo> properties = new ArrayList<>();
            //开关
            if (deviceInfo.has("count")) {
                int lightCount = deviceInfo.get("count").asInt();
                properties.add(new DeviceInfo("count", lightCount));
            }
            //开关状态
            if (deviceInfo.has("switch1")) {
                properties.add(new DeviceInfo("switch1", deviceInfo.get("switch1").asText()));
            }
            //电压
            if (deviceInfo.has("voltage")) {
                properties.add(new DeviceInfo("voltage", deviceInfo.get("voltage").asDouble()));
            }
            //电流
            if (deviceInfo.has("current")) {
                properties.add(new DeviceInfo("current", deviceInfo.get("current").asDouble()));
            }
            //功率
            if (deviceInfo.has("power")) {
                properties.add(new DeviceInfo("power", deviceInfo.get("power").asDouble()));
            }
            //累计
            if (deviceInfo.has("total")) {
                properties.add(new DeviceInfo("total", deviceInfo.get("total").asDouble()));
            }
            return properties;
        }
        return null;
    }
}
