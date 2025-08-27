package com.youlai.boot.dashBoard.model.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-08-27  10:22
 *@Description: TODO
 */
@Data
@ColumnWidth(20)
public class ExportRoomRank {
    @ExcelProperty(value = "房间名称")
    private String roomName;
    @ExcelProperty(value = "用电量")
    private Double totalElectricity;
//    @ExcelProperty(value = "分类名称")
//    private String categoryName;
}
