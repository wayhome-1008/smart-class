package com.youlai.boot.device.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-16  12:46
 *@Description: TODO
 */
@Data
public class HumanRadarSensorParams {
    /**
     * 1 有人 0 无人
     */
    @JSONField(name = "Occupancy")
    private Integer occupancy;
    /**
     * 有人到无人的延时时间 单位为秒
     */
    @JSONField(name = "OccupiedDelay")
    private Integer occupiedDelay;
    /**
     * 无人到有人的延时时间
     */
    @JSONField(name = "UnoccupiedDelay")
    private Integer unoccupiedDelay;
    /**
     * 1 运动 2 呼吸 3 微动   数字越小，越不灵敏
     */
    @JSONField(name = "OccupiedThreshold ")
    private Integer occupiedThreshold ;
}
