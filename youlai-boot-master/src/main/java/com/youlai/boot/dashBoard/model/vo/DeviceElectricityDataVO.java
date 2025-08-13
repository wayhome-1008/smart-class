package com.youlai.boot.dashBoard.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 *@Author: way
 *@CreateTime: 2025-08-13  16:31
 *@Description: TODO
 */
@Data
@AllArgsConstructor
public class DeviceElectricityDataVO {
    private Double totalElectricity;
    private Instant time;
}
