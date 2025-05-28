package com.youlai.boot.device.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  09:34
 *@Description: 仅使用于根据设备code查询设备信息及info属性的factory使用
 */
@Data
@AllArgsConstructor
public class DeviceInfo {
    private String name;   // 统一属性名（如 "temperature", "humidity"）
    private Object value;  // 属性值（数值/布尔/字符串）
}
