package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-06-04  11:53
 *@Description: TODO
 */
public class ZigBeeSmartPlugParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        if (deviceInfo!=null){
            List<DeviceInfo> properties = new ArrayList<>();
            if (deviceInfo.has("outlet1")) {
                properties.add(new DeviceInfo("outlet1", deviceInfo.get("outlet1").asInt()));
                properties.add(new DeviceInfo("count", 1));
            }
            if (deviceInfo.has("switch1")) {
                properties.add(new DeviceInfo("switch1", deviceInfo.get("switch1").asText().equals("on") ? "ON" : "OFF"));
            }
            return properties;
        }
        return null;
    }
}
