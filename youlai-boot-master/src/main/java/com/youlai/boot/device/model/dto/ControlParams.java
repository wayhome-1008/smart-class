package com.youlai.boot.device.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-29  17:54
 *@Description: TODO
 */
@Data
public class ControlParams {
    @JsonProperty("switches")  // 显式声明JSON字段名（非必须但更清晰）
    private List<Switch> switches;
}
