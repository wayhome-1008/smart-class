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

    //总用电量
    @Column(name = "Total")
    private Double total;

    //昨日用电量
    @Column(name = "Yesterday")
    private Double yesterday;

    //今日用电量
    @Column(name = "Today")
    private Double today;

    @Column(name = "Period")
    private Double period;

    //有功功率
    @Column(name = "Power")
    private Integer power;

    //视在功率
    @Column(name = "ApparentPower")
    private Integer apparentPower;

    //无功功率
    @Column(name = "ReactivePower")
    private Integer reactivePower;

    //功率因数
    @Column(name = "Factor")
    private Double factor;

    //电压
    @Column(name = "Voltage")
    private Double voltage;

    //电流
    @Column(name = "Current")
    private Double current;

    @Column(timestamp = true)
    private Instant time;

    private Double kilowattHour;
}
