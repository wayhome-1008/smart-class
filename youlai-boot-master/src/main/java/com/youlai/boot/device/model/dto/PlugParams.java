package com.youlai.boot.device.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-07  15:00
 *@Description: mqtt协议：计量插座发送的数据嵌套json中的params
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlugParams {
    /**
     * 所有通道的开关状态
     */
    @JSONField(name = "switches")
    private List<Switch> switches;

    /**
     *  电压 ABC 代表三相电的ABC 三相，其中单相电设备用 RMS_VoltageA，单位为 V(伏)，精确到 0.1V
     */
    @JSONField(name = "RMS_VoltageA")
    private Double rmsVoltageA;

    @JSONField(name = "RMS_VoltageB")
    private Double rmsVoltageB;

    @JSONField(name = "RMS_VoltageC")
    private Double rmsVoltageC;

    /**
     * 电流 ABC 代表三相电的ABC 三相，其中 单相电设备用 RMS_CurrentA，单位为 A(安培)，精确到 0.001A，即 1mA
     */
    @JSONField(name = "RMS_CurrentA")
    private Double rmsCurrentA;

    @JSONField(name = "RMS_CurrentB")
    private Double rmsCurrentB;

    @JSONField(name = "RMS_CurrentC")
    private Double rmsCurrentC;

    /**
     * 有功功率 ABC 代表三相电的ABC 三相，其中单相电设备用 activePowerA，单位为 W(瓦)，精确到 1W
     */
    @JSONField(name = "activePowerA")
    private Double activePowerA;

    @JSONField(name = "activePowerB")
    private Double activePowerB;

    @JSONField(name = "activePowerC")
    private Double activePowerC;

    /**
     * 总有功电量，单位为 kWh（千瓦时），精确到 0.1kWh，即 0.1 度电
     */
    @JSONField(name = "electricalEnergy")
    private Double electricalEnergy;
}
