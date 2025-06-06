package com.youlai.boot.device.model.influx;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;

import java.time.Instant;

/**
 *@Author: way
 *@CreateTime: 2025-06-06  11:48
 *@Description: TODO
 */
@Data
@Measurement(name = "device")
public class InfluxSensor {
    @Column(tag = true)
    private String deviceCode;

    @Column(name = "battery")
    private Integer battery;

    @Column(name = "temperature")
    private Integer temperature;

    @Column(name = "humidity")
    private Integer humidity;

    @Column(name = "illuminance")
    private Integer illuminance;

    @Column(timestamp = true)
    private Instant time;
}
