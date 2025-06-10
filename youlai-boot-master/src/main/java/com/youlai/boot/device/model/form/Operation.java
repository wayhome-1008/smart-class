package com.youlai.boot.device.model.form;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-06-10  16:52
 *@Description: TODO
 */
@Data
public class Operation {
    @Pattern(regexp = "device|room|floor", message = "错误类型")
    private String type;
    private Long id;
    @Pattern(regexp = "ON|OFF", message = "错误操作")
    private String operate;
}
