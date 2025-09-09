package com.youlai.boot.system.model.dto;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-09-09  11:00
 *@Description: TODO
 */
@Data
public class DeptImportDTO {
    @ExcelProperty(value = "部门名称")
    private String name;

    @ExcelProperty(value = "部门编号")
    private String code;

    @ExcelProperty(value = "父部门编号")
    private String parentCode;
}
