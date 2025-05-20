package com.youlai.boot.device.model.dto;

import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-19  16:33
 *@Description: TODO
 */
@Data
public class GateWayManage {
    private String sequence;
    private String cmd;
    private GateWayManageParams params;
}
