package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  11:12
 *@Description: ZigBee协议开关设备解析
 */
@Slf4j
public class ZigBeeSwitchParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        if (deviceInfo != null) {
            List<DeviceInfo> properties = new ArrayList<>();
            ObjectNode switches = JsonNodeFactory.instance.objectNode();
            Iterator<String> fieldNames = deviceInfo.fieldNames();
            int switchCount = 0;
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (fieldName.startsWith("switch")) {
                    switchCount++;
                    String freePostingStatus = deviceInfo.get(fieldName).asText();
                    switches.put(fieldName, freePostingStatus);
                    properties.add(new DeviceInfo(fieldName, freePostingStatus));
                }
            }
            properties.add(new DeviceInfo("count", switchCount));
            return properties;
        }
        return null;
    }
}
