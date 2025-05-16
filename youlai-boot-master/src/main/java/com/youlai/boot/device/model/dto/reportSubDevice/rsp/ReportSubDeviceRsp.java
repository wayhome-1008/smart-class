package com.youlai.boot.device.model.dto.reportSubDevice.rsp;

import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-16  12:06
 *@Description: TODO
 */
@Data
public class ReportSubDeviceRsp {
    private Integer sequence;
    private Integer error;
    private ReportSubDeviceRspParams params;
}
