package com.youlai.boot.device.handler.zigbee;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.SubUpdateSensorRsp;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.youlai.boot.common.util.JsonUtils.mergeJson;
import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;

/**
 *@Author: way
 *@CreateTime: 2025-04-27  11:55
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubUpdateHandler implements MsgHandler {
    private final DeviceService deviceService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        try {
            JSONObject deviceInfo = JSON.parseObject(jsonMsg);
            int sequence = deviceInfo.getIntValue("sequence");
            String originalMac = deviceInfo.getString("deviceId");
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, originalMac);
            if (ObjectUtils.isEmpty(device)) {
                device = deviceService.getByCode(originalMac);
            }
            if (ObjectUtil.isNotEmpty(device)) {
                //传感器
                if (device.getDeviceTypeId() == 2 || device.getDeviceTypeId() == 5) {
                    processSensor(topic, mqttClient, device, jsonMsg, sequence);
                }
                //计量插座
                if (device.getDeviceTypeId() == 4) {
                    processPlug(topic, mqttClient, device, jsonMsg, sequence);
                }
                //随意贴
                if (device.getDeviceTypeId() == 3) {
                    processFreePosting(topic, mqttClient, device, jsonMsg, sequence);
                }
                //人体存在传感器(微波)
                if (device.getDeviceTypeId() == 6) {
                    processHumanRadarSensor(topic, mqttClient, device, jsonMsg, sequence);
                }
                //三路开关
                if (device.getDeviceTypeId() == 7) {
                    processThreeWaySwitch(topic, mqttClient, device, jsonMsg, sequence);
                }
            }


        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }
    /**
     * @description: 随意贴
     * @author: way
     * @date: 2025/5/27 17:36
     **/
    private void processThreeWaySwitch(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            if (device != null) {
                device.setDeviceInfo(mergeJson);
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                deviceService.updateById(device);
                RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
            }
        }
    }

    private void processFreePosting(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            if (device != null) {
                device.setDeviceInfo(mergeJson);
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                deviceService.updateById(device);
                RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
            }

        }

    }

    private void processPlug(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            if (device != null) {
                device.setDeviceInfo(mergeJson);
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                deviceService.updateById(device);
                RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
            }

        }
    }

    private void processHumanRadarSensor(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            if (device != null) {
                device.setDeviceInfo(mergeJson);
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                deviceService.updateById(device);
                RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
            }
        }
    }

    private void processSensor(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            //存redis
            if (device != null) {
                device.setDeviceInfo(mergeJson);
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                //目前直接再存库
                deviceService.updateById(device);
                RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
            }
        }
    }

    private static void RspMqtt(String topic, MqttClient mqttClient, String deviceId, int sequence) throws MqttException {
        SubUpdateSensorRsp subUpdateSensorRsp = new SubUpdateSensorRsp();
        subUpdateSensorRsp.setError(0);
        subUpdateSensorRsp.setSequence(sequence);
        subUpdateSensorRsp.setDeviceId(deviceId);
        mqttClient.publish(topic + "_rsp", JSON.toJSONString(subUpdateSensorRsp).getBytes(), 2, false);
    }

    @Override
    public HandlerType getType() {
        return HandlerType.SUB_UPDATE;
    }
}
