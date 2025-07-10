package com.youlai.boot.dashBoard.model.vo;

import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-07-10  10:32
 *@Description: TODO
 */
@Data
public class RoomElectricityRankingVO {
    private Integer rank;
    private Long roomId;
    private String roomCode;
    private String roomName;
    private Double totalElectricity;


}
