package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.service.DeviceInfoParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  11:07
 *@Description: ZigBee协议随意贴设备解析
 */
public class ZigBeeFreePostingParser implements DeviceInfoParser {
    @Override
    public List<DeviceInfo> parse(JsonNode deviceInfo) {
      if (deviceInfo!= null){
          List<DeviceInfo> properties = new ArrayList<>();
          //电量
          if (deviceInfo.has("battery")) {
              properties.add(new DeviceInfo("battery", deviceInfo.get("battery").asInt()));
          }
          ObjectNode switches = JsonNodeFactory.instance.objectNode();
          Iterator<String> fieldNames = deviceInfo.fieldNames();
          int count = 0;
          while (fieldNames.hasNext()) {
              String fieldName = fieldNames.next();
              if (fieldName.startsWith("switch")) {
                  count++;
                  String freePostingStatus = deviceInfo.get(fieldName).asText();
                  switches.put(fieldName, freePostingStatus);
                  properties.add(new DeviceInfo(fieldName, freePostingStatus));
              }
          }
          properties.add(new DeviceInfo("count", count));
          return properties;
      }
      return null;
    }
}
