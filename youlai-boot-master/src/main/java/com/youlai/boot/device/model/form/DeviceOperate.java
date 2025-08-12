package com.youlai.boot.device.model.form;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  17:14
 *@Description: TODO
 */
@Data
@ToString
public class DeviceOperate {
    @Pattern(regexp = "ON|OFF", message = "错误操作")
    private String operate;
    private String way;
    private Integer count;
}
