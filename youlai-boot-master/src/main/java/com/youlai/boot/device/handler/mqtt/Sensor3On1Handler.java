package com.youlai.boot.device.handler.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.youlai.boot.common.util.JsonUtils.mergeJson;
import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;
import static com.youlai.boot.common.util.MacUtils.getCodeByTopic;

/**
 *@Author: way
 *@CreateTime: 2025-06-03  12:25
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class Sensor3On1Handler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException, JsonProcessingException {
        try {
            //topic是code 唯一的
            //截取code
            String deviceCode = getCodeByTopic(topic);
            //从设备缓存获取看是否存在
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (ObjectUtils.isEmpty(device)) {
                device = deviceService.getByCode(deviceCode);
            }
            if (ObjectUtils.isNotEmpty(device)) {
                JsonNode jsonNode = stringToJsonNode(jsonMsg);
                JsonNode mergeJson = mergeJson(device.getDeviceInfo(), jsonNode);
                device.setDeviceInfo(mergeJson);
                Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
                if (ObjectUtils.isNotEmpty(deviceCache)) {
                    device.setStatus(1);
                    redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
                    deviceService.updateById(device);
                } else {
                    device.setStatus(1);
                    deviceService.updateById(device);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.SENSOR3ON1;
    }
}
