package com.youlai.boot.device.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.topic.HandlerType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 *@Author: way
 *@CreateTime: 2025-04-25  11:46
 *@Description: TODO
 */
@Component
@Slf4j
public class DeviceMessageHandler implements MsgHandler {

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        log.info("接收到网关注册消息：主题{},内容{}", topic, jsonMsg);
        //网关请求注册
        JSONObject recordMap = JSON.parseObject(jsonMsg, JSONObject.class);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sequence", recordMap.get("sequence"));
        hashMap.put("error", 0);
        log.info("发送消息:{}", hashMap);
        try {
            mqttClient.publish("/zbgw/9454c5ee8180/register_rsp", JSON.toJSONString(hashMap).getBytes(), 2, false);
//            mqttProducer.send("/zbgw/9454c5ee8180/register_rsp", 2, false, String.valueOf(hashMap));
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.REGISTER;
    }
}
