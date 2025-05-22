package com.youlai.boot.device.model.dto.event.rsp;

import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-22  10:20
 *@Description: TODO
 */
@Data
public class DeviceEventResult {
    private List<SubDevicesResult> subDevices;
}
