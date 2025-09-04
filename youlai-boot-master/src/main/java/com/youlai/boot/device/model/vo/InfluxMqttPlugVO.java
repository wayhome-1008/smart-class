package com.youlai.boot.device.model.vo;

import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-06-26  14:46
 *@Description: TODO
 */
@Data
public class InfluxMqttPlugVO {
    private List<String> time;  // 时间戳

    private List<Double> value; // 指标值
}
