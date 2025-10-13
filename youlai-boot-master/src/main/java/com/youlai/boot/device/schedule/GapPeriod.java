package com.youlai.boot.device.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 *@Author: way
 *@CreateTime: 2025-10-13  10:20
 *@Description: TODO
 */
@Data
@AllArgsConstructor
public class GapPeriod {
    private  Instant start; // 空白期开始时间
    private  Instant end;   // 空白期结束时间
}
