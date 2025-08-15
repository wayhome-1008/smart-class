package com.youlai.boot.dashBoard.model.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-08-15  11:11
 *@Description: TODO
 */
@Data
@ColumnWidth(20)
public class RoomsElectricityVO {
    @ExcelProperty(value = "房间号")
    private Long roomId;
    @ExcelProperty(value = "房间名称")
    private String roomName;
    @ExcelProperty(value = "用电量")
    private Double totalElectricity;
}
