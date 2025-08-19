package com.youlai.boot.dashBoard.model.vo;

import lombok.Data;

import java.time.Instant;

/**
 *@Author: way
 *@CreateTime: 2025-08-18  16:59
 *@Description: TODO
 */
@Data
public class InfluxMqttPlugStats {
    private Instant time;         // 窗口时间
    private Double firstTotal;    // 窗口内Total最早值
    private Double lastTotal;     // 窗口内Total最晚值
    private Double avgVoltage;    // 窗口内Voltage平均值
    private Double avgCurrent;    // 窗口内Current平均值
}
