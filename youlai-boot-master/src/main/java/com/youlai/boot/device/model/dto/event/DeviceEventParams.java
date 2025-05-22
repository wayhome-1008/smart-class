package com.youlai.boot.device.model.dto.event;

import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-22  10:00
 *@Description: TODO
 */
@Data
public class DeviceEventParams {
    private List<SubDevicesEvent> subDevices;
}
