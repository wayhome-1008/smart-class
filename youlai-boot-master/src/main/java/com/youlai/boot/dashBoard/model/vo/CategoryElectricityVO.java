package com.youlai.boot.dashBoard.model.vo;

import lombok.Data;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-07-11  15:54
 *@Description: TODO
 */
@Data
public class CategoryElectricityVO {
//    private List<String> categoryName;
//    private List<String> time;  // 时间戳
//    private List<Double> value; // 指标值
// 时间维度 ["周一","周二",...,"周日"]
private List<String> times;

    // 分类数据列表
    private List<CategoryData> data;

    @Data
    public static class CategoryData {
        private String categoryName;
        // 对应时间维度的值 [1.1, 1.2, ..., 1.7]
        private List<Double> values;
    }
}
