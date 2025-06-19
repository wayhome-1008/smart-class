package com.youlai.boot.device.handler.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

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

    }



    @Override
    public HandlerType getType() {
        return HandlerType.SENSOR3ON1;
    }
}
