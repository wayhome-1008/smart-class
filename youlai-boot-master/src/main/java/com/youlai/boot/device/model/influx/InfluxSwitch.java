package com.youlai.boot.device.model.influx;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;

import java.time.Instant;

/**
 *@Author: way
 *@CreateTime: 2025-07-03  09:56
 *@Description: TODO
 */
@Data
@Measurement(name = "device")
public class InfluxSwitch {
    @Column(tag = true)
    private String deviceCode;

    @Column(tag = true)
    private String roomId;

    @Column(name = "switch")
    private String switchState;



    @Column(timestamp = true)
    private Instant time;
}
