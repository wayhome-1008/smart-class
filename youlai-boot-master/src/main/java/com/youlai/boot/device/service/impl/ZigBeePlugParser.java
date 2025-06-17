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
      if (deviceInfo!=null){
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
              if (plugData.has("RMS_VoltageA")) {
                  properties.add(new DeviceInfo("RMS_VoltageA", plugData.get("RMS_VoltageA").asInt()));
              }
              if (plugData.has("activePowerA")) {
                  properties.add(new DeviceInfo("activePowerA", plugData.get("activePowerA").asInt()));
              }
              if (plugData.has("RMS_CurrentA")) {
                  properties.add(new DeviceInfo("RMS_CurrentA", plugData.get("RMS_CurrentA").asInt()));
              }
          }
          return properties;
      }
      return null;
    }
}
