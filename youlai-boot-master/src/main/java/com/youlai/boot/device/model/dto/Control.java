package com.youlai.boot.device.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-29  17:15
 *@Description: TODO
 */
@Data
public class Control {
    @JsonProperty("sequence")
    private String sequence;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("params")
    private ControlParams params;
}
