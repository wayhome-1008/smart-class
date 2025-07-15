package com.youlai.boot.device.model.form;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-07-09  10:04
 *@Description: ZigBee串口设备发送数据Json类
 */
@Data
public class SerialDataDown {
    private Integer sequence;
    private String deviceId;
    @JSONField(name="params")
    private SerialData serialData;
}
