package com.youlai.boot.device.handler.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;

import static com.youlai.boot.common.util.JsonUtils.mergeJson;
import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;
import static com.youlai.boot.common.util.MacUtils.getCodeByTopic;

/**
 *@Author: way
 *@CreateTime: 2025-06-18  15:49
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ResultHandler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        try {
            // 1. 转换消息为JSON
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            String deviceCode = getCodeByTopic(topic);

            // 2. 获取设备信息（缓存优先）
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (ObjectUtils.isEmpty(device)) {
                device = deviceService.getByCode(deviceCode);
            }
            if (device.getDeviceTypeId() == 4) {
                plug(jsonNode, device, deviceCode);
            }
            if (device.getDeviceTypeId() == 8) {
                light(jsonNode, device, deviceCode);
            }

        } catch (Exception e) {
            log.error("设备 {} 处理失败: {}", topic, e.getMessage(), e);
            throw new RuntimeException("灯光状态处理异常", e);
        }
    }

    private void plug(JsonNode jsonNode, Device device, String deviceCode) {
        ObjectNode lightStatus = JsonNodeFactory.instance.objectNode();
        String power = jsonNode.get("POWER").asText();
        lightStatus.put("switch1", power);
        lightStatus.put("count", 1);
        JsonNode mergedInfo = mergeJson(device.getDeviceInfo(), lightStatus);
        device.setDeviceInfo(mergedInfo);
        // 双写：Redis缓存 + 数据库
        device.setStatus(1);
        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
//        deviceService.updateById(device);
    }

    private void light(JsonNode jsonNode, Device device, String deviceCode) {
        // 3. 动态处理所有灯光路数
        ObjectNode lightStatus = JsonNodeFactory.instance.objectNode();
        Iterator<String> fieldNames = jsonNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (fieldName.startsWith("POWER")) {
                if (fieldName.equals("POWER")) {
                    String status = jsonNode.get(fieldName).asText();
                    lightStatus.put("switch1", status);
                } else {
                    String status = jsonNode.get(fieldName).asText();
                    lightStatus.put(fieldName.replace("POWER", "switch"), status);
                    log.debug("灯光路数 {} 状态: {}", fieldName, status);
                }

            }
        }
        // 4. 更新设备信息
        if (ObjectUtils.isNotEmpty(device)) {
            JsonNode mergedInfo = mergeJson(device.getDeviceInfo(), lightStatus);
            device.setDeviceInfo(mergedInfo);
            // 双写：Redis缓存 + 数据库
            device.setStatus(1);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
//            deviceService.updateById(device);
            log.info("设备 {} 灯光状态更新完成", deviceCode);
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.RESULT;
    }
}
