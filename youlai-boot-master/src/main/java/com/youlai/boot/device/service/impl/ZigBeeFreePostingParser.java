package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  11:07
 *@Description: ZigBee协议随意贴设备解析
 */
public class ZigBeeFreePostingParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        List<DeviceInfo> properties = new ArrayList<>();
        if (deviceInfo.has("params")) {
            JsonNode freePostingData = deviceInfo.get("params");
            //电量
            if (freePostingData.has("battery")) {
                properties.add(new DeviceInfo("battery", freePostingData.get("battery").asInt()));
            }
            //点击 双击 长摁
            if (freePostingData.has("key")) {
                properties.add(new DeviceInfo("key", freePostingData.get("key").asInt()));
            }
            //最后点击
            if (freePostingData.has("outlet")) {
                properties.add(new DeviceInfo("outlet", freePostingData.get("outlet").asInt()));
            }
        }
        return properties;
    }
}
