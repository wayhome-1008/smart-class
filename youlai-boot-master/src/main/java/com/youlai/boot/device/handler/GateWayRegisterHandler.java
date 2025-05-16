package com.youlai.boot.device.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static com.youlai.boot.device.handler.SubUpdateHandler.deviceList;

/**
 *@Author: way
 *@CreateTime: 2025-04-25  11:46
 *@Description: ZigBee网关启动上线后会发送请求注册消息 需根据数据库中设备查询是否可以同意注册
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GateWayRegisterHandler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        log.info("接收到网关注册消息：主题{},内容{}", topic, jsonMsg);
        //网关请求注册
        JSONObject recordMap = JSON.parseObject(jsonMsg, JSONObject.class);
        String macAddress = MacUtils.parseMACAddress(recordMap.getJSONObject("params").getString("wifimac"));
        //如果mac在deviceList存在
        Device deviceEntity = deviceList.stream().filter(device -> macAddress.equals(device.getDeviceMac())).findFirst().orElse(null);
        if (ObjectUtils.isNotEmpty(deviceEntity)) {
            sendRegister(mqttClient, recordMap);
            //將緩存中該數據刪除
            redisTemplate.opsForHash().delete(RedisConstants.MqttDevice.GateWay, macAddress);
            return;
        } else {
            //从redis获取
//            JSONObject jsonObjectCache = (JSONObject) redisTemplate.opsForHash().get(RedisConstants.MqttDevice.GateWay, macAddress);
            redisTemplate.opsForHash().put(RedisConstants.MqttDevice.GateWay, macAddress, recordMap);
        }
//        sendRegister(mqttClient, recordMap);
    }

    private static void sendRegister(MqttClient mqttClient, JSONObject recordMap) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sequence", recordMap.get("sequence"));
        hashMap.put("error", 0);
        try {
            mqttClient.publish("/zbgw/" + recordMap.getJSONObject("params").getString("wifimac") + "/register_rsp", JSON.toJSONString(hashMap).getBytes(), 2, false);
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.REGISTER;
    }
}
