package com.youlai.boot.dashBoard.model.vo;

import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-09-24  11:25
 *@Description: TODO
 */
@Data
public class CategoryElectricityData {
    private List<String> time;    // 时间标签列表
    private List<Double> value;   // 用电量值列
}
