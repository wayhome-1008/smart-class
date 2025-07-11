package com.youlai.boot.common.util;

import java.text.DecimalFormat;

/**
 *@Author: way
 *@CreateTime: 2025-07-10  13:06
 *@Description: TODO
 */
public class MathUtils {
    public static Double formatDouble(Double value) {
        if (value == null) {
            return null;
        }
        DecimalFormat df = new DecimalFormat("#.##"); // 保留两位小数，自动四舍五入
        return Double.parseDouble(df.format(value));
    }
}
