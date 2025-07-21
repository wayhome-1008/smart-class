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
            if (deviceInfo.has("DHT11")) {
                JsonNode data = deviceInfo.get("DHT11");
                //温度
                if (data.has("temperature")) {
                    properties.add(new DeviceInfo("temperature", data.get("temperature").asDouble()));
                }
                //湿度
                if (data.has("humidity")) {
                    properties.add(new DeviceInfo("humidity", data.get("humidity").asDouble()));
                }
            }
            if (deviceInfo.has("BH1750")) {
                JsonNode light = deviceInfo.get("BH1750");
                //亮度
                if (light.has("illuminance")) {
                    properties.add(new DeviceInfo("illuminance", light.get("illuminance").asDouble()));
                }
            }
            //人
            if (deviceInfo.has("LD2402")) {
                JsonNode person = deviceInfo.get("LD2402");
                //亮度
                if (person.has("Distance")) {
                    properties.add(new DeviceInfo("Distance", person.get("Distance").asDouble()));
                }
            }
            return properties;
        }
        return null;
    }
}
