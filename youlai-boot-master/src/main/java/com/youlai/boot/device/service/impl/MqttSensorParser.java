package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  16:05
 *@Description: MQTT协议温湿度传感器设备解析
 */
public class MqttSensorParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
    if (deviceInfo!=null){
        List<DeviceInfo> properties = new ArrayList<>();
        if (deviceInfo.has("DHT11")) {
            JsonNode data = deviceInfo.get("DHT11");
            //温度
            if (data.has("temperature")) {
                properties.add(new DeviceInfo("temperature", data.get("Temperature").asDouble()));
            }
            //湿度
            if (data.has("humidity")) {
                properties.add(new DeviceInfo("humidity", data.get("Humidity").asDouble()));
            }
        }
        return properties;
    }
    return null;
    }
}
