package com.youlai.boot.device.model.form;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-07-09  10:06
 *@Description: TODO
 */
@Data
public class SerialData {
    @JSONField(name="TunnelingDataDown")
    private String data;
    @JSONField(name="tunnelingID")
    private Integer tunnelingId;
}
