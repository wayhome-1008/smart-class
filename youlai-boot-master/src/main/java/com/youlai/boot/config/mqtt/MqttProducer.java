package com.youlai.boot.config.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @description: 发送mqtt消息
 * @author: way
 * @date: 2024/7/22 15:04
 * @param:
 * @return:
 **/
@Component
@Slf4j
public class MqttProducer {
    @Autowired
    private MqttClient mqttClient;

    public void send(String topic, int qos, boolean retained, String payload) throws MqttException {
        mqttClient.publish(topic, payload.getBytes(), qos, retained);
    }

}