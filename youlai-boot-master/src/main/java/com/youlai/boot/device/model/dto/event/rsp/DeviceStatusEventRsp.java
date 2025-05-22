package com.youlai.boot.device.model.dto.event.rsp;

import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-22  10:13
 *@Description: TODO
 */
@Data
public class DeviceStatusEventRsp {
    private Integer error;
    private String sequence;
    private DeviceEventResult result;
}
