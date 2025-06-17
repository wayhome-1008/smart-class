package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
            int count = 0;
            if (!deviceInfo.isMissingNode()) {
                Iterator<Map.Entry<String, JsonNode>> fields = deviceInfo.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    properties.add(new DeviceInfo(entry.getKey(), entry.getValue().asText()));
                    count++;
                }
            }
            try {
                count = count / 2;
            } catch (Exception e) {
                log.error("解析zigbee开关数据异常", e);
            } finally {
                properties.add(new DeviceInfo("count", count));
            }
            return properties;
        }
        return null;
    }
}
