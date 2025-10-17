package com.youlai.boot.device.handler.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.handler.status.DeviceStatusManager;
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
import java.util.Optional;

import static com.youlai.boot.common.util.JsonUtils.mergeJson;
import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;
import static com.youlai.boot.common.util.MacUtils.getCodeByTopic;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  16:53
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StateHandler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;
    private final DeviceStatusManager deviceStatusManager;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        //从缓存去设备
        String deviceCode = getCodeByTopic(topic);
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (device == null) {
            device = deviceService.getByCode(deviceCode);
        }
//        deviceStatusManager.updateDeviceOnlineStatus(deviceCode);
        //计量插座
        if (device.getDeviceTypeId() == 4) {
            handlerPlug(topic, jsonMsg);
        }
        //灯光
        if (device.getDeviceTypeId() == 7) {
            handlerLight(topic, jsonMsg);
        }
    }

    private void handlerPlug(String topic, String jsonMsg) {
        try {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            String deviceCode = getCodeByTopic(topic);
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (ObjectUtils.isNotEmpty(device)) {
                String power = jsonNode.get("POWER").asText();
                ObjectNode metrics = JsonNodeFactory.instance.objectNode();
                //接受得数据与旧数据合并)
                metrics.put("count", 1);
                metrics.put("switch1", power);
                JsonNode mergeJson = mergeJson(Optional.of(device).map(Device::getDeviceInfo).orElse(null), metrics);
                device.setDeviceInfo(mergeJson);
                device.setStatus(1);
//                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                deviceStatusManager.updateDeviceOnlineStatus(device.getDeviceCode(), device);
            }
        } catch (Exception e) {
            log.error("设备 {} 处理失败: {}", topic, e.getMessage(), e);
            throw new RuntimeException("插座状态处理异常", e);
        }
    }

    private void handlerLight(String topic, String jsonMsg) {
        try {
            // 1. 转换消息为JSON
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            String deviceCode = getCodeByTopic(topic);

            // 2. 获取设备信息（缓存优先）
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            //定义灯光路数
            int lightCount = 0;
            // 3. 动态处理所有灯光路数
            ObjectNode metrics = JsonNodeFactory.instance.objectNode();
            Iterator<String> fieldNames = jsonNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (fieldName.startsWith("POWER")) {
                    lightCount++;
                }
            }
            metrics.put("count", lightCount);
            // 4. 更新设备信息
            if (ObjectUtils.isNotEmpty(device)) {
                JsonNode mergedInfo = mergeJson(device.getDeviceInfo(), metrics);
                device.setDeviceInfo(mergedInfo);
                Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
                if (deviceCache != null) {
//                    redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
                    deviceStatusManager.updateDeviceOnlineStatus(device.getDeviceCode(), device);
                }
//                log.info("设备 {} 灯光状态更新完成", deviceCode);
            }
        } catch (Exception e) {
            log.error("设备 {} 处理失败: {}", topic, e.getMessage(), e);
            throw new RuntimeException("灯光状态处理异常", e);
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.STATE;
    }
}
