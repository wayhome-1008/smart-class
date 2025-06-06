package com.youlai.boot.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.influxdb.annotations.Measurement;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *@Author: way
 *@CreateTime: 2025-06-05  10:37
 *@Description: TODO
 */
@Slf4j
public class FluxUtil {
    // ============ JSON处理方法 ============
    public static Point flattenJson(JsonNode node, Point point) {
        Map<String, Object> result = new HashMap<>();
        flattenJson("", node, result);
        return flattenPoint(result, point);
    }

    private static void flattenJson(String prefix, JsonNode node, Map<String, Object> result) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenJson(key, entry.getValue(), result);
            });
        } else if (node.isValueNode()) {
            if (node.isTextual()) {
                result.put(prefix, node.asText());
            } else if (node.isBoolean()) {
                result.put(prefix, node.asBoolean());
            } else if (node.isNumber()) {
                result.put(prefix, node.numberValue());
            }
        }
    }

    private static Point flattenPoint(Map<String, Object> result, Point point) {
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number) {
                point.addField(entry.getKey(), ((Number) value).doubleValue());
            } else if (value instanceof String) {
                point.addField(entry.getKey(), (String) value);
            } else if (value instanceof Boolean) {
                point.addField(entry.getKey(), (Boolean) value);
            }
        }
        log.info("写入json{}", result);
        return point;
    }
}
