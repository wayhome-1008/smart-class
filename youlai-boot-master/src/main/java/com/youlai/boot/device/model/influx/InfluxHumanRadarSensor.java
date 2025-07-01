package com.youlai.boot.device.model.influx;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;

import java.time.Instant;

/**
 *@Author: way
 *@CreateTime: 2025-06-12  16:36
 *@Description: TODO
 */
@Data
@Measurement(name = "device")
public class InfluxHumanRadarSensor {
    @Column(tag = true)
    private String deviceCode;

    @Column(tag = true)
    private Long roomId;

    @Column(name = "battery")
    private Integer battery;

    @Column(name = "motion")
    private Integer motion;

    @Column(timestamp = true)
    private Instant time;
}
