package com.youlai.boot.config.handler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *@Author: way
 *@CreateTime: 2025-05-22  12:53
 *@Description: Device实体 JsonNode属性处理器
 */
public class JsonTypeHandler extends AbstractJsonTypeHandler<JsonNode> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected JsonNode parse(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    @Override
    protected String toJson(JsonNode obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }
}
