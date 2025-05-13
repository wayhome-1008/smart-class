package com.youlai.boot.device.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *@Author: way
 *@CreateTime: 2025-05-07  15:01
 *@Description: 表示开关信息的数据结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Switch {
    /**
     * 通道状态，取值为 "on" 或 "off"
     */
    @JSONField(name = "switch")
    private String switchStatus;

    /**
     * 通道编号，目前固定为 0
     */
    @JSONField(name = "outlet")
    private Integer outlet;
}
