package com.youlai.boot.device.handler.zigbee;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
                device.setStatus(1);
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
                //插座
                if (device.getDeviceTypeId() == 10) {
                    processSocket(topic, mqttClient, device, jsonMsg, sequence);
                }
            }


        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    private void processSocket(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            //获取params
            JsonNode params = jsonNode.get("params");
            JsonNode switchesArray = params.get("switches");
            // 1. 准备存储所有开关状态的对象
            ObjectNode allSwitchStates = JsonNodeFactory.instance.objectNode();
            // 2. 遍历switches数组
            if (switchesArray.isArray()) {
                for (JsonNode switchNode : switchesArray) {
                    int outletNum = switchNode.get("outlet").asInt() + 1; // 转为1-based编号
                    String switchState = switchNode.get("switch").asText();
                    //大小写转换
                    if (switchState.equals("on")) {
                        switchState = "ON";
                    } else if (switchState.equals("off")) {
                        switchState = "OFF";
                    }
                    // 3. 存储每个开关状态
                    allSwitchStates.put("outlet" + outletNum, outletNum);
                    allSwitchStates.put("switch" + outletNum, switchState);
                }
            }
            if (device != null) {
                JsonNode mergeJson = mergeJson(Optional.of(device).map(Device::getDeviceInfo).orElse(null), allSwitchStates);
                device.setDeviceInfo(mergeJson);
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                deviceService.updateById(device);
                RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
            }
        }
    }

    /**
     * @description: 开关
     * @author: way
     * @date: 2025/5/27 17:36
     **/
    private void processThreeWaySwitch(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            //获取params
            JsonNode params = jsonNode.get("params");
            JsonNode switchesArray = params.get("switches");
            // 1. 准备存储所有开关状态的对象
            ObjectNode allSwitchStates = JsonNodeFactory.instance.objectNode();
            // 2. 遍历switches数组
            if (switchesArray.isArray()) {
                for (JsonNode switchNode : switchesArray) {
                    int outletNum = switchNode.get("outlet").asInt() + 1; // 转为1-based编号
                    String switchState = switchNode.get("switch").asText();
                    //大小写转换
                    if (switchState.equals("on")) {
                        switchState = "ON";
                    } else if (switchState.equals("off")) {
                        switchState = "OFF";
                    }
                    // 3. 存储每个开关状态
                    allSwitchStates.put("outlet" + outletNum, outletNum);
                    allSwitchStates.put("switch" + outletNum, switchState);
                }
            }
            if (device != null) {
                JsonNode mergeJson = mergeJson(Optional.of(device).map(Device::getDeviceInfo).orElse(null), allSwitchStates);
                device.setDeviceInfo(mergeJson);
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                deviceService.updateById(device);
                RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
            }
        }
    }

    /**
     * @description: 随意贴
     * @author: way
     * @date: 2025/6/3 10:19
     **/
    private void processFreePosting(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            //获取params
            JsonNode params = jsonNode.get("params");
            // 1. 准备存储所有开关状态的对象
            ObjectNode allSwitchStates = JsonNodeFactory.instance.objectNode();
            JsonNode mergeJson;
            //2.根据返回的key决定是几路
            if (params.has("outlet")) {
                int way = params.get("outlet").asInt() + 1;
                allSwitchStates.put("switch" + way, params.get("key").asText());
            }
            if (params.has("battery")) {
                allSwitchStates.put("battery", params.get("battery").asText());
            }
            mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), allSwitchStates);
            if (device != null) {
                device.setDeviceInfo(mergeJson);
                Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, device.getDeviceCode());
                if (deviceCache != null) {
                    try {
                        //并且是电量
                        if (params.has("battery")) {
                            //查看缓存和新的电量是否一致
                            if (!deviceCache.getDeviceInfo().get("battery").asText().equals(params.get("battery").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                    } catch (NullPointerException e) {
                        log.error(e.getMessage());
                        deviceService.updateById(device);
                    }
                }
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
            }

        }

    }

    private void processPlug(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            //获取params
            JsonNode params = jsonNode.get("params");
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            if (device != null) {
                device.setDeviceInfo(mergeJson);
                Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, device.getDeviceCode());
                try {
                    if (deviceCache != null) {
                        if (params.has("activePowerA")) {
                            //查看缓存和新的电量是否一致
                            if (!deviceCache.getDeviceInfo().get("activePowerA").asText().equals(params.get("activePowerA").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("activePowerB")) {
                            //查看缓存和新的电量是否一致
                            if (!deviceCache.getDeviceInfo().get("activePowerB").asText().equals(params.get("activePowerB").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("activePowerC")) {
                            //查看缓存和新的电量是否一致
                            if (!deviceCache.getDeviceInfo().get("activePowerC").asText().equals(params.get("activePowerC").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("RMS_VoltageA")) {
                            if (!deviceCache.getDeviceInfo().get("RMS_VoltageA").asText().equals(params.get("RMS_VoltageA").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("RMS_VoltageB")) {
                            if (!deviceCache.getDeviceInfo().get("RMS_VoltageB").asText().equals(params.get("RMS_VoltageB").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("RMS_VoltageC")) {
                            if (!deviceCache.getDeviceInfo().get("RMS_VoltageC").asText().equals(params.get("RMS_VoltageC").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("RMS_CurrentA")) {
                            if (!deviceCache.getDeviceInfo().get("RMS_CurrentA").asText().equals(params.get("RMS_CurrentA").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("RMS_CurrentB")) {
                            if (!deviceCache.getDeviceInfo().get("RMS_CurrentB").asText().equals(params.get("RMS_CurrentB").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("RMS_CurrentC")) {
                            if (!deviceCache.getDeviceInfo().get("RMS_CurrentC").asText().equals(params.get("RMS_CurrentC").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("electricalEnergy")) {
                            if (!deviceCache.getDeviceInfo().get("electricalEnergy").asText().equals(params.get("electricalEnergy").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                    }
                } catch (NullPointerException e) {
                    log.error(e.getMessage());
                    deviceService.updateById(device);
                } finally {
                    redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                    RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
                }

            }

        }
    }

    private void processHumanRadarSensor(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            //获取params
            JsonNode params = jsonNode.get("params");
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            if (device != null) {
                device.setDeviceInfo(mergeJson);
                Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, device.getDeviceCode());
                if (deviceCache != null) {
                    try {
                        //并且是电量
                        if (params.has("battery")) {
                            //查看缓存和新的电量是否一致
                            if (!deviceCache.getDeviceInfo().get("battery").asText().equals(params.get("battery").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("motion")) {
                            //查看缓存和新的电量是否一致
                            if (!deviceCache.getDeviceInfo().get("motion").asText().equals(params.get("motion").asText())) {
                                deviceService.updateById(device);
                            }
                        }  //并且是电量
                        if (params.has("battery")) {
                            //查看缓存和新的电量是否一致
                            if (!deviceCache.getDeviceInfo().get("battery").asText().equals(params.get("battery").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                        if (params.has("motion")) {
                            //查看缓存和新的电量是否一致
                            if (!deviceCache.getDeviceInfo().get("motion").asText().equals(params.get("motion").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                    } catch (NullPointerException e) {
                        log.error(e.getMessage());
                        deviceService.updateById(device);
                    } finally {
                        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                        RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
                    }
                }
            }
        }
    }

    private void processSensor(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            //获取params
            JsonNode params = jsonNode.get("params");
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            //存redis
            if (device != null) {
                device.setDeviceInfo(mergeJson);
                Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, device.getDeviceCode());
                if (deviceCache != null) {
                    try {
                        //并且是电量
                        if (params.has("battery")) {
                            //查看缓存和新的电量是否一致
                            if (!deviceCache.getDeviceInfo().get("battery").asText().equals(params.get("battery").asText())) {
                                deviceService.updateById(device);
                            }
                        }
                    } catch (NullPointerException e) {
                        deviceService.updateById(device);
                    } finally {
                        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                        RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
                    }
                }
            }
        }
    }

    private static void RspMqtt(String topic, MqttClient mqttClient, String deviceId, int sequence) throws
            MqttException {
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
