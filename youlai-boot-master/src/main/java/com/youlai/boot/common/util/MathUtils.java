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
        if (value <= 0) {
            return 0.0;
        }
        DecimalFormat df = new DecimalFormat("0.00"); // 保留两位小数，自动四舍五入
        return Double.valueOf(df.format(value));
    }
}
