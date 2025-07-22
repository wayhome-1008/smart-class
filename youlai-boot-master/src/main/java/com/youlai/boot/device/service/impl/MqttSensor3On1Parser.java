package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *@Author: way
 *@CreateTime: 2025-06-03  12:26
 *@Description: TODO
 */
public class MqttSensor3On1Parser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        if (deviceInfo != null) {
            List<DeviceInfo> properties = new ArrayList<>();
            //温度
            if (deviceInfo.has("temperature")) {
                properties.add(new DeviceInfo("temperature", deviceInfo.get("temperature").asDouble()));
            }
            //湿度
            if (deviceInfo.has("humidity")) {
                properties.add(new DeviceInfo("humidity", deviceInfo.get("humidity").asDouble()));
            }
            //亮度
            if (deviceInfo.has("illuminance")) {
                properties.add(new DeviceInfo("illuminance", deviceInfo.get("illuminance").asDouble()));
            }
            //人
            if (deviceInfo.has("motion")) {
                properties.add(new DeviceInfo("motion", deviceInfo.get("motion").asDouble()));
            }
            return properties;
        }
        return null;
    }
}
