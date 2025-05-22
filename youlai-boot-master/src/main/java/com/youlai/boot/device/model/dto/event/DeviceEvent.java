package com.youlai.boot.device.model.dto.event;

import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-22  09:56
 *@Description: TODO
 */
@Data
public class DeviceEvent {
        private String sequence;
        private String event;
        private DeviceEventParams params;
}
