package com.youlai.boot.device.model.influx;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;

import java.time.Instant;

/**
 *@Author: way
 *@CreateTime: 2025-06-20  10:44
 *@Description: MQTT计量插座InfluxDB模型
 */
@Data
@Measurement(name = "device")
public class InfluxMqttPlug {
    @Column(tag = true)
    private String deviceCode;

    @Column(name = "Total")
    private Double total;

    @Column(name = "Yesterday")
    private Double yesterday;

    @Column(name = "Today")
    private Double today;

    @Column(name = "Period")
    private Double period;

    @Column(name = "Power")
    private Integer power;

    @Column(name = "ApparentPower")
    private Integer apparentPower;

    @Column(name = "ReactivePower")
    private Integer reactivePower;

    @Column(name = "Factor")
    private Double factor;

    @Column(name = "Voltage")
    private Double voltage;

    @Column(name = "Current")
    private Double current;

    @Column(timestamp = true)
    private Instant time;
}
