package com.youlai.boot.scene.model.form;

import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-07-30  17:32
 *@Description: 阈值条件（属性 + 操作符 + 阈值）
 */
@Data
public class ThresholdCondition {
    private String property; // 设备属性（如：temperature）
    private String operator; // 操作符（=、!=、>、<、>=、<=）
    private Object value; // 阈值（如：30、"online"）
}
