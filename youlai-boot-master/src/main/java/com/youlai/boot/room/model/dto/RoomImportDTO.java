package com.youlai.boot.room.model.dto;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-09-09  14:43
 *@Description: TODO
 */
@Data
public class RoomImportDTO {
    @ExcelProperty(value = "楼宇名称(必填)")
    private String buildingName;

    @ExcelProperty(value = "楼宇编号(必填)")
    private String buildingCode;

    @ExcelProperty(value = "楼层号(必填)")
    private String floorNumber;

    @ExcelProperty(value = "房间名称(必填)")
    private String classRoomCode;

    @ExcelProperty(value = "部门名称(非必填)")
    private String deptName;
}
