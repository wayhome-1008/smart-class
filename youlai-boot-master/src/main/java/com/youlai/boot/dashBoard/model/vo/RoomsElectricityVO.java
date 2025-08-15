package com.youlai.boot.dashBoard.model.vo;

import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-08-15  11:11
 *@Description: TODO
 */
@Data
public class RoomsElectricityVO {
    private Long roomId;
    private String roomName;
    private Double totalElectricity;
}
