package com.youlai.boot.device.model.dto.reportSubDevice;

import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-16  10:55
 *@Description: TODO
 */
@Data
public class ReportSubDeviceParams {
    private Integer totalNumber;
    private Integer index;
    private List<SubDevice> subDevices;
}
