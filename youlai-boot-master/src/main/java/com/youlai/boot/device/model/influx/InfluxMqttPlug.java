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

    @Column(tag = true)
    private String roomId;

    @Column(tag = true)
    private String deviceType;

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

    @Column(name = "activePowerA")
    private Double activePowerA;

//    @Column(name = "activePowerB")
//    private Integer activePowerB;
//
//    @Column(name = "activePowerC")
//    private Integer activePowerC;

    @Column(name = "RMS_VoltageA")
    private Double RMS_VoltageA;

//    @Column(name = "RMS_VoltageB")
//    private Integer RMS_VoltageB;
//
//    @Column(name = "RMS_VoltageC")
//    private Integer RMS_VoltageC;

    @Column(name = "RMS_CurrentA")
    private Double RMS_CurrentA;

//    @Column(name = "RMS_CurrentB")
//    private Integer RMS_CurrentB;
//
//    @Column(name = "RMS_CurrentC")
//    private Integer RMS_CurrentC;

    @Column(name = "electricalEnergy")
    private Double electricalEnergy;

    @Column(timestamp = true)
    private Instant time;

    private Double kilowattHour;

    @Column(name = "switch")
    private String switchState;
}
