package com.youlai.boot.device.handler.zigbee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.handler.status.DeviceStatusManager;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.youlai.boot.common.util.MacUtils.extractFromTopic;

/**
 *@Author: way
 *@CreateTime: 2025-06-17  16:20
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ManageStatusHandler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceStatusManager deviceStatusManager;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException, JsonProcessingException {
        //说明在线
        String deviceCode = extractFromTopic(topic);
        Device gateWay = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (ObjectUtils.isNotEmpty(gateWay)) {
            deviceStatusManager.updateDeviceOnlineStatus(gateWay.getDeviceCode(), gateWay);
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.MANAGE_RSP;
    }
}
