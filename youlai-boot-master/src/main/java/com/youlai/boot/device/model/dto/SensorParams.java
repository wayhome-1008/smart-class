package com.youlai.boot.device.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *@Author: way
 *@CreateTime: 2025-05-07  11:38
 *@Description: mqtt协议：传感器发送的数据嵌套json中的params
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorParams {
    /**
     * 移动事件，0 表示无人，1 表示有人
     */
    @JSONField(name = "motion")
    private Integer motion;

    /**
     * 门窗的开关状态，0 表示无门，1 表示开门
     */
    @JSONField(name = "lock")
    private Integer lock;

    /**
     * 是否发生水浸事件，0 表示无水，1 表示水浸
     */
    @JSONField(name = "water")
    private Integer water;

    /**
     * 按键触发事件，0 表示长按，1 表示双击，2 表示单击
     */
    @JSONField(name = "key")
    private Integer key;

    /**
     * 烟雾告警事件，0 表示无烟，1 表示有烟
     */
    @JSONField(name = "smoke")
    private Integer smoke;

    /**
     * 温度值，实际温度 = 温度值 / 100，取值范围 [-2500 - 12500]
     */
    @JSONField(name = "temperature")
    private String temperature;

    /**
     * 湿度值，实际湿度百分比 = 湿度值 / 100，取值范围 [0 - 10000]
     */
    @JSONField(name = "humidity")
    private String humidity;

    /**
     * 电池电量百分比，取值范围 [0, 100]
     */
    @JSONField(name = "battery")
    private Integer battery;

    /**
     * 紧急事件，值为 1 表示紧急按钮触发
     */
    @JSONField(name = "Emergency")
    private Integer emergency;

    /**
     * 光照强度，单位 lux 取值范围 [0,83000]
     */
    @JSONField(name = "Illuminance")
    private String illuminance;
}
