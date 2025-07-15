package com.youlai.boot.device.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-07-03  11:16
 *@Description: TODO
 */
@Data
public class InfluxSwitchVO {

    @JsonProperty("switch")
    private String switchStatus;

    @JsonProperty("way")
    private String way;

    @JsonProperty("time")
    private String time;
}
