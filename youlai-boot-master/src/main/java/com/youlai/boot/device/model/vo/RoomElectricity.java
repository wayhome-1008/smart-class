package com.youlai.boot.device.model.vo;

import com.influxdb.annotations.Column;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-07-04  14:37
 *@Description: TODO
 */
@Data
public class RoomElectricity {
    //总用电量
    private Double total;
    private Double voltage;
    private Double current;
}
