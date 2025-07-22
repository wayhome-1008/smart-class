package com.youlai.boot.device.service.impl;

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
        if (deviceInfo!=null){
            List<DeviceInfo> properties = new ArrayList<>();
                //电量
                if (deviceInfo.has("battery")) {
                    properties.add(new DeviceInfo("battery", deviceInfo.get("battery").asInt()));
                }
                //温度
                if (deviceInfo.has("temperature")) {
                    properties.add(new DeviceInfo("temperature", deviceInfo.get("temperature").asDouble()));
                }
                //湿度
                if (deviceInfo.has("humidity")) {
                    properties.add(new DeviceInfo("humidity", deviceInfo.get("humidity").asDouble()));
                }
                //光照
                if (deviceInfo.has("illuminance")) {
                    properties.add(new DeviceInfo("illuminance", deviceInfo.get("illuminance").asDouble()));
                }

            return properties;
        }
        return null;
    }
}
