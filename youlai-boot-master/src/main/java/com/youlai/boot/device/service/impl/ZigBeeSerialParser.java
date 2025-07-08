package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-07-08  16:31
 *@Description: TODO
 */
public class ZigBeeSerialParser  implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        return List.of();
    }
}
