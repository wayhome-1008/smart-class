package com.youlai.boot.scene.liteFlow;

import com.youlai.boot.scene.model.form.ThresholdCondition;
import lombok.extern.slf4j.Slf4j;

/**
 *@Author: way
 *@CreateTime: 2025-07-30  18:03
 *@Description: 阈值比较器，用于比较设备实际值与设定阈值
 */
@Slf4j
public class ThresholdComparator {
    public static boolean compare(ThresholdCondition condition, String actualStr) {
        String operator = condition.getOperator();
        Object thresholdValue = condition.getValue();

        // 统一转为字符串比较
        String thresholdStr = thresholdValue.toString();
        // 数值类型比较
        try {
            double actualNum = Double.parseDouble(actualStr);
            double thresholdNum = Double.parseDouble(thresholdStr);
            return compareNumber(operator, actualNum, thresholdNum);
        } catch (NumberFormatException e) {
            // 字符串类型比较
            return compareString(operator, actualStr, thresholdStr);
        }
    }

    private static boolean compareNumber(String operator, double actual, double threshold) {
        return switch (operator) {
            case "=" -> actual == threshold;
            case "!=" -> actual != threshold;
            case ">" -> actual > threshold;
            case "<" -> actual < threshold;
            case ">=" -> actual >= threshold;
            case "<=" -> actual <= threshold;
            default -> throw new IllegalArgumentException("不支持的操作符: " + operator);
        };
    }

    private static boolean compareString(String operator, String actual, String threshold) {
        return switch (operator) {
            case "=" -> actual.equals(threshold);
            case "!=" -> !actual.equals(threshold);
            case ">" -> actual.compareTo(threshold) > 0;
            case "<" -> actual.compareTo(threshold) < 0;
            case ">=" -> actual.compareTo(threshold) >= 0;
            case "<=" -> actual.compareTo(threshold) <= 0;
            default -> throw new IllegalArgumentException("字符串不支持的操作符: " + operator);
        };
    }
}
