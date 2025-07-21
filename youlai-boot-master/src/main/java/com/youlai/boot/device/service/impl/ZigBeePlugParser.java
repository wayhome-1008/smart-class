package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  11:06
 *@Description: ZigBee协议计量插座设备解析
 */
public class ZigBeePlugParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        if (deviceInfo != null) {
            List<DeviceInfo> properties = new ArrayList<>();
            if (deviceInfo.has("params")) {
                JsonNode plugData = deviceInfo.get("params");
                JsonNode switchesArray = plugData.get("switches");
                if (switchesArray.isArray()) {
                    for (JsonNode switchNode : switchesArray) {
                        // 3. 存储每个开关状态
                        properties.add(new DeviceInfo("outlet1", switchNode.get("outlet").asInt()));
                        properties.add(new DeviceInfo("switch1", switchNode.get("switch").asText().equals("on") ? "ON" : "OFF"));
                        properties.add(new DeviceInfo("count", 1));
                    }
                }
                if (plugData.has("voltage")) {
                    properties.add(new DeviceInfo("voltage", plugData.get("voltage").asDouble()));
                }
                if (plugData.has("power")) {
                    properties.add(new DeviceInfo("power", plugData.get("power").asDouble()));
                }
                if (plugData.has("current")) {
                    properties.add(new DeviceInfo("current", plugData.get("current").asDouble()));
                }
                if (plugData.has("total")) {
                    properties.add(new DeviceInfo("total", plugData.get("total").asDouble()));
                }
            }
            return properties;
        }
        return null;
    }
}
