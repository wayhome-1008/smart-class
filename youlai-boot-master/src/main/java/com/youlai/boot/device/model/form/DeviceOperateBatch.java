package com.youlai.boot.device.model.form;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-09-26  11:37
 *@Description: TODO
 */
@Data
public class DeviceOperateBatch {
    private String deviceId;
//    @Pattern(regexp = "ON|OFF", message = "错误操作")
//    private String operate;
//    private String way;
//    private Integer count;
}
