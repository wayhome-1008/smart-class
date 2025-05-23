package com.youlai.boot.device.handler.mqtt;

import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 *@Author: way
 *@CreateTime: 2025-05-23  17:05
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SensorHandler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException {
        //topic是code 唯一的
        //截取code
//        topic.
//        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceId);
    }

    @Override
    public HandlerType getType() {
        return HandlerType.SENSOR;
    }
}
