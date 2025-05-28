package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  15:52
 *@Description: ZigBee协议人体存在雷达设备解析
 */
public class ZigBeeHumanRadarSensorParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        List<DeviceInfo> properties = new ArrayList<>();
        if (deviceInfo.has("params")) {
            JsonNode sensorData = deviceInfo.get("params");
            //电量
            if (sensorData.has("battery")) {
                properties.add(new DeviceInfo("battery", sensorData.get("battery").asInt()));
            }
            //是否有人
            if (sensorData.has("motion")) {
                properties.add(new DeviceInfo("motion", sensorData.get("motion").asInt()));
            }
        }
        return properties;
    }
}
