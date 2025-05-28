package com.youlai.boot.device.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  09:36
 *@Description: ZigBee协议温湿度传感器设备解析
 */
public class ZigBeeSensorParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        List<DeviceInfo> properties = new ArrayList<>();
        if (deviceInfo.has("params")) {
            JsonNode sensorData = deviceInfo.get("params");
            //电量
            if (sensorData.has("battery")) {
                properties.add(new DeviceInfo("battery", sensorData.get("battery").asInt()));
            }
            //温度
            if (sensorData.has("temperature")) {
                properties.add(new DeviceInfo("temperature", sensorData.get("temperature").asDouble() / 100));
            }
            //湿度
            if (sensorData.has("humidity")) {
                properties.add(new DeviceInfo("humidity", sensorData.get("humidity").asDouble() / 100));
            }
            //光照
            if (sensorData.has("Illuminance")) {
                properties.add(new DeviceInfo("Illuminance", sensorData.get("Illuminance").asDouble() / 100));
            }
        }
        return properties;
    }
}
