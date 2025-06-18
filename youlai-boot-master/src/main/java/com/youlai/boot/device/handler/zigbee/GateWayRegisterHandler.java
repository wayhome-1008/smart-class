package com.youlai.boot.device.handler.zigbee;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.util.MacUtils;
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

import java.util.HashMap;

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
    private final DeviceService deviceService;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        log.info("接收到网关注册消息：主题{},内容{}", topic, jsonMsg);
        //网关请求注册
        JSONObject recordMap = JSON.parseObject(jsonMsg, JSONObject.class);
        String originalMac = recordMap.getJSONObject("params").getString("wifimac");
        String macAddress = MacUtils.parseMACAddress(originalMac);
        //查缓存是否存在
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, originalMac);
        if (ObjectUtils.isEmpty(device)) {
            device = deviceService.getByMac(macAddress);
        }
        //此时不空则注册 否则说明网关在发请求注册 但是设备还不存在
        if (ObjectUtils.isNotEmpty(device)) {
            sendRegister(mqttClient, recordMap);
        } else {
            //存缓存
            Device deviceCache = new Device();
            deviceCache.setDeviceMac(macAddress);
            deviceCache.setDeviceCode(originalMac);
            deviceCache.setStatus(1);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, originalMac, deviceCache);
        }
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
