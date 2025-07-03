package com.youlai.boot.device.model.vo;

import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-07-02  15:52
 *@Description: TODO
 */
@Data
public class InfluxMotionVO {
    private List<String> time;  // 时间戳
    private List<Double> value; // 指标值
}
