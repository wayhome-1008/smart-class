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
        String property = condition.getProperty();

        // 统一转为字符串比较
        String thresholdStr = thresholdValue.toString();

        log.info("开始比较阈值 - 属性: {}, 实际值: {}, 操作符: {}, 阈值: {}",
                property, actualStr, operator, thresholdStr);

        // 数值类型比较
        try {
            double actualNum = Double.parseDouble(actualStr);
            double thresholdNum = Double.parseDouble(thresholdStr);
            boolean result = compareNumber(operator, actualNum, thresholdNum);
//            log.info("数值比较结果 - 实际值: {}, 阈值: {}, 操作符: {}, 结果: {}",
//                    actualNum, thresholdNum, operator, result);
            return result;
        } catch (NumberFormatException e) {
            // 字符串类型比较
            boolean result = compareString(operator, actualStr, thresholdStr);
//            log.info("字符串比较结果 - 实际值: {}, 阈值: {}, 操作符: {}, 结果: {}",
//                    actualStr, thresholdStr, operator, result);
            return result;
        }
    }

    private static boolean compareNumber(String operator, double actual, double threshold) {
        boolean result = switch (operator) {
            case "=" -> actual == threshold;
            case "!=" -> actual != threshold;
            case ">" -> actual > threshold;
            case "<" -> actual < threshold;
            case ">=" -> actual >= threshold;
            case "<=" -> actual <= threshold;
            default -> throw new IllegalArgumentException("不支持的操作符: " + operator);
        };

        log.info("执行数值比较 - 操作符: {}, 实际值: {}, 阈值: {}, 结果: {}",
                operator, actual, threshold, result);

        return result;
    }

    private static boolean compareString(String operator, String actual, String threshold) {
        boolean result = switch (operator) {
            case "=" -> actual.equals(threshold);
            case "!=" -> !actual.equals(threshold);
            case ">" -> actual.compareTo(threshold) > 0;
            case "<" -> actual.compareTo(threshold) < 0;
            case ">=" -> actual.compareTo(threshold) >= 0;
            case "<=" -> actual.compareTo(threshold) <= 0;
            default -> throw new IllegalArgumentException("字符串不支持的操作符: " + operator);
        };

        log.info("执行字符串比较 - 操作符: {}, 实际值: '{}', 阈值: '{}', 结果: {}",
                operator, actual, threshold, result);

        return result;
    }
}
