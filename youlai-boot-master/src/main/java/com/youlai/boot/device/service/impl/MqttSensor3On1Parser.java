package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-06-03  12:26
 *@Description: TODO
 */
public class MqttSensor3On1Parser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        List<DeviceInfo> properties = new ArrayList<>();
        if (deviceInfo.has("DHT11")) {
            JsonNode data = deviceInfo.get("DHT11");
            //温度
            if (data.has("Temperature")) {
                properties.add(new DeviceInfo("temperature", data.get("Temperature").asDouble()));
            }
            //湿度
            if (data.has("Humidity")) {
                properties.add(new DeviceInfo("humidity", data.get("Humidity").asDouble()));
            }
             //光照
        }
        return properties;
    }
}
