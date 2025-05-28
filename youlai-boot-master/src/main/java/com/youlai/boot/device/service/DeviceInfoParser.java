package com.youlai.boot.device.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.device.model.vo.DeviceInfo;

import java.util.List;

public interface DeviceInfoParser {
    /**
     * 解析 deviceInfo JSON，生成标准化属性列表
     * @param deviceInfo 原始 JSON 数据（如 {"temp_raw": 5000, "hum_raw": 3000}）
     * @return 标准化属性列表
     */
    List<DeviceInfo> parse(JsonNode deviceInfo);
}
