package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-06-20  10:02
 *@Description: MQTT计量插座解析器
 */
public class MqttPlugParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
        if (deviceInfo != null) {
            List<DeviceInfo> properties = new ArrayList<>();
            //开关
            if (deviceInfo.has("count")) {
                int lightCount = deviceInfo.get("count").asInt();
                properties.add(new DeviceInfo("count", lightCount));
                properties.add(new DeviceInfo("switch1", deviceInfo.get("switch1").asText()));
            }
            if (deviceInfo.has("StatusSNS")) {
                JsonNode statusSNS = deviceInfo.get("StatusSNS");
                if (statusSNS.has("ENERGY")) {
                    JsonNode energy = statusSNS.get("ENERGY");
                    properties.add(new DeviceInfo("ApparentPower", energy.get("ApparentPower").asInt()));
                    properties.add(new DeviceInfo("ReactivePower", energy.get("ReactivePower").asInt()));
                    properties.add(new DeviceInfo("Factor", energy.get("Factor").asDouble()));
                    properties.add(new DeviceInfo("Voltage", energy.get("Voltage").asDouble()));
                    properties.add(new DeviceInfo("Current", energy.get("Current").asDouble()));
                    properties.add(new DeviceInfo("Power", energy.get("Power").asInt()));
                    properties.add(new DeviceInfo("Today", energy.get("Today").asDouble()));
                    properties.add(new DeviceInfo("Yesterday", energy.get("Yesterday").asDouble()));
                    properties.add(new DeviceInfo("Total", energy.get("Total").asDouble()));
                    properties.add(new DeviceInfo("Yesterday", energy.get("Yesterday").asDouble()));
//                    properties.add(new DeviceInfo("Period", energy.get("Period").asDouble()));
                    properties.add(new DeviceInfo("Power", energy.get("Power").asInt()));
                }
            }
            return properties;
        }
        return null;
    }
}
