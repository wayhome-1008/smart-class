package com.youlai.boot.device.model.vo;

import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-06-26  14:46
 *@Description: TODO
 */
@Data
public class InfluxMqttPlugVO {
    private String time;  // 时间戳
    private Double value; // 指标值
}
