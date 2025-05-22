package com.youlai.boot.device.model.dto.event;

import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-22  10:02
 *@Description: TODO
 */
@Data
public class SubDevicesEvent {
    private String deviceId;
    private Boolean online;
}
