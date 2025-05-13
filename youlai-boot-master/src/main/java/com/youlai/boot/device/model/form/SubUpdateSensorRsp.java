package com.youlai.boot.device.model.form;

import lombok.Getter;
import lombok.Setter;

/**
 *@Author: way
 *@CreateTime: 2025-04-24  12:34
 *@Description: TODO
 */
@Setter
@Getter
public class SubUpdateSensorRsp {
    private Integer error;
    private Integer sequence;
    private String deviceId;
}
