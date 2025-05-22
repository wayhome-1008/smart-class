package com.youlai.boot.device.handler.service;

import com.youlai.boot.device.topic.HandlerType;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public interface MsgHandler {
    // 处理消息的方法
    void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException;

    // 获取处理器对应的枚举类型
    HandlerType getType();
}
