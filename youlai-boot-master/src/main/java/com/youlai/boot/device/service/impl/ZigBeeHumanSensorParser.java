package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  15:49
 *@Description: ZigBee协议人体感应雷达设备解析
 */
public class ZigBeeHumanSensorParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        if (deviceInfo != null) {
            List<DeviceInfo> properties = new ArrayList<>();
            //电量
            if (deviceInfo.has("battery")) {
                properties.add(new DeviceInfo("battery", deviceInfo.get("battery").asInt()));
            }
            //是否有人
            if (deviceInfo.has("motion")) {
                properties.add(new DeviceInfo("motion", deviceInfo.get("motion").asInt()));
            }
            return properties;
        }
        return null;
    }
}
