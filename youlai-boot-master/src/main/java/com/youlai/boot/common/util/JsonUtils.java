package com.youlai.boot.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

/**
 *@Author: way
 *@CreateTime: 2025-05-22  14:41
 *@Description: TODO
 */
public class JsonUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 递归合并多层级JSON（支持任意深度嵌套）
     * @param original 原始JSON节点（可为null，表示新增场景）
     * @param update 更新的JSON节点（可为null，表示不修改）
     * @return 合并后的JSON节点
     */
    public static JsonNode mergeJson(JsonNode original, JsonNode update) {
        try {
            // 场景1：更新节点为null → 直接返回原始节点（不修改）
            if (update == null || update.isNull()) {
                return original != null ? original : OBJECT_MAPPER.createObjectNode();
            }

            // 场景2：原始节点为null → 直接返回更新节点（新增场景）
            if (original == null || original.isNull()) {
                return update.deepCopy();
            }
            // 场景3：两者均为对象节点 → 递归合并子字段
            if (original.isObject() && update.isObject()) {
                ObjectNode originalObj = (ObjectNode) original;
                ObjectNode updateObj = (ObjectNode) update;

                // 遍历更新节点的所有子字段
                Iterator<Map.Entry<String, JsonNode>> fields = updateObj.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String fieldName = entry.getKey();
                    JsonNode originalField = originalObj.get(fieldName);  // 原始节点的子字段
                    JsonNode updateField = entry.getValue();              // 更新节点的子字段

                    // 递归合并子字段
                    JsonNode mergedField = mergeJson(originalField, updateField);
                    if (mergedField.isNull()) {
                        originalObj.remove(fieldName);  // 合并结果为null → 删除该字段
                    } else {
                        originalObj.set(fieldName, mergedField);  // 合并结果非null → 设置字段
                    }
                }
                return originalObj;
            }

            // 场景4：两者均为数组节点 → （可选）覆盖或合并数组（示例采用覆盖，可根据需求调整）
            if (original.isArray() && update.isArray()) {
                ArrayNode originalArray = (ArrayNode) original;
                originalArray.removeAll();  // 清空原始数组
                originalArray.addAll((ArrayNode) update);  // 添加更新数组的所有元素
                return originalArray;
            }
            // 场景5：其他类型（字符串、数字等）→ 直接用更新值覆盖原始值
            return update.deepCopy();
        } catch (Exception e) {
            throw new RuntimeException("JSON合并失败", e);
        }
    }

    /**
     * 将JSON字符串转换为JsonNode对象
     * @param jsonStr JSON格式的字符串（如{"name":"张三","age":25}）
     * @return 解析后的JsonNode对象
     * @throws JsonProcessingException 字符串格式错误时抛出异常
     */
    public static JsonNode stringToJsonNode(String jsonStr) throws JsonProcessingException {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON字符串不能为空");
        }
        return OBJECT_MAPPER.readTree(jsonStr);  // 核心方法：解析字符串为JsonNode
    }
}
