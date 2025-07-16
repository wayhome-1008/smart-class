package com.youlai.boot.dashBoard.model.vo;

import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-22  17:19
 *@Description: TODO
 */
@Data
public class DashCount {
    //设备数量
    private Long deviceCount;
    //用户数量
    private Long userCount;
    //日志数量
    private Long logCount;
    //房间的数量
    private Long roomCount;
    //todo
    private Long demo1Count;
    private Integer demo2Count;
}
