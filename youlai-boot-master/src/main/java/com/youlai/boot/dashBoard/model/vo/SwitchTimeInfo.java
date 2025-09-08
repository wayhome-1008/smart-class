package com.youlai.boot.dashBoard.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 *@Author: way
 *@CreateTime: 2025-09-08  16:16
 *@Description: TODO
 */
@Data
@AllArgsConstructor
public class SwitchTimeInfo {
    private String earliestOnTime;
    private String latestOffTime;
}
