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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

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
    public static List<Device> deviceList = new ArrayList<>();

    @PostConstruct
    public void init() {
        deviceList = deviceService.getDeviceList();
    }

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        try {
            JSONObject deviceInfo = JSON.parseObject(jsonMsg);
            int sequence = deviceInfo.getIntValue("sequence");
            String deviceId = deviceInfo.getString("deviceId");
            if (ObjectUtil.isNotEmpty(deviceList)) {
                Device device1 = deviceList.stream().filter(device -> deviceId.equals(device.getDeviceCode())).findFirst().orElse(null);
                if (ObjectUtil.isNotEmpty(device1)) {
                    //传感器
                    if (device1.getDeviceTypeId() == 2 || device1.getDeviceTypeId() == 5) {
                        processSensor(topic, mqttClient, deviceId, jsonMsg, sequence);
                    }
                    //计量插座
                    if (device1.getDeviceTypeId() == 4) {
                        processPlug(topic, mqttClient, deviceId, jsonMsg, sequence);
                    }
                    //随意贴
                    if (device1.getDeviceTypeId() == 3) {
                        processFreePosting(topic, mqttClient, deviceId, jsonMsg, sequence);
                    }
                    //人体存在传感器(微波)
                    if (device1.getDeviceTypeId() == 6) {
                        processHumanRadarSensor(topic, mqttClient, deviceId, jsonMsg, sequence);
                    }
                    //三路开关
                    if (device1.getDeviceTypeId() == 7) {
                        processThreeWaySwitch(topic, mqttClient, deviceId, jsonMsg, sequence);
                    }
                }
            }

        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    private void processThreeWaySwitch(String topic, MqttClient mqttClient, String deviceId, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        //根据mqtt发来的deviceId实际是数据库的deviceCode查询缓存device
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceId);
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            device.setDeviceInfo(mergeJson);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceId, device);
            deviceService.updateById(device);
            RspMqtt(topic, mqttClient, deviceId, sequence);
        }
    }

    private void processFreePosting(String topic, MqttClient mqttClient, String deviceId, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        //根据mqtt发来的deviceId实际是数据库的deviceCode查询缓存device
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceId);
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            device.setDeviceInfo(mergeJson);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceId, device);
            deviceService.updateById(device);
            RspMqtt(topic, mqttClient, deviceId, sequence);
        }

    }

    private void processPlug(String topic, MqttClient mqttClient, String deviceId, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        //根据mqtt发来的deviceId实际是数据库的deviceCode查询缓存device
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceId);
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            device.setDeviceInfo(mergeJson);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceId, device);
            deviceService.updateById(device);
            RspMqtt(topic, mqttClient, deviceId, sequence);
        }
    }

    private void processHumanRadarSensor(String topic, MqttClient mqttClient, String deviceId, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        //根据mqtt发来的deviceId实际是数据库的deviceCode查询缓存device
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceId);
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            device.setDeviceInfo(mergeJson);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceId, device);
            deviceService.updateById(device);
            RspMqtt(topic, mqttClient, deviceId, sequence);
        }
    }

    private void processSensor(String topic, MqttClient mqttClient, String deviceId, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        //根据deviceId查询出redis的设备信息并把info存储在json
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceId);
        if (ObjectUtil.isNotEmpty(device)) {
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            JsonNode mergeJson = mergeJson(Optional.ofNullable(device).map(Device::getDeviceInfo).orElse(null), jsonNode);
            //存redis
            device.setDeviceInfo(mergeJson);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceId, device);
            //目前直接再存库
            deviceService.updateById(device);
            RspMqtt(topic, mqttClient, deviceId, sequence);
        }

    }

    private static void RspMqtt(String topic, MqttClient mqttClient, String deviceId, int sequence) throws MqttException {
        SubUpdateSensorRsp subUpdateSensorRsp = new SubUpdateSensorRsp();
        subUpdateSensorRsp.setError(0);
        subUpdateSensorRsp.setSequence(sequence);
        subUpdateSensorRsp.setDeviceId(deviceId);
        mqttClient.publish(topic + "_rsp", JSON.toJSONString(subUpdateSensorRsp).getBytes(), 2, false);
    }

//    private void processHumanRadarSensorOld(String topic, MqttClient mqttClient, String deviceId, String deviceInfo, int sequence) throws MqttException {
//        log.info("接收到人体存在传感器信息：{}", deviceInfo);
//        HumanRadarSensor humanRadarSensor = JSONObject.parseObject(deviceInfo, HumanRadarSensor.class);
//        HumanRadarSensor humanRadarSensorCache = (HumanRadarSensor) redisTemplate.opsForHash().get(RedisConstants.MqttDevice.DEVICE, deviceId);
//        if (ObjectUtil.isEmpty(humanRadarSensorCache)) {
//            redisTemplate.opsForHash().put(RedisConstants.MqttDevice.DEVICE, deviceId, humanRadarSensor);
//        } else {
//            if (humanRadarSensor.getParams().getOccupancy() != null) {
//                humanRadarSensorCache.getParams().setOccupancy(humanRadarSensor.getParams().getOccupancy());
//            } else if (humanRadarSensor.getParams().getOccupiedDelay() != null) {
//                humanRadarSensorCache.getParams().setOccupiedDelay(humanRadarSensor.getParams().getOccupiedDelay());
//            } else if (humanRadarSensor.getParams().getUnoccupiedDelay() != null) {
//                humanRadarSensorCache.getParams().setUnoccupiedDelay(humanRadarSensor.getParams().getUnoccupiedDelay());
//            } else if (humanRadarSensor.getParams().getOccupiedThreshold() != null) {
//                humanRadarSensorCache.getParams().setOccupiedThreshold(humanRadarSensor.getParams().getOccupiedThreshold());
//            }
//        }
//        humanRadarSensor.setParams(humanRadarSensorCache.getParams());
//        redisTemplate.opsForHash().put(RedisConstants.MqttDevice.DEVICE, deviceId, humanRadarSensor);
//        SubUpdateSensorRsp subUpdateSensorRsp = new SubUpdateSensorRsp();
//        subUpdateSensorRsp.setError(0);
//        subUpdateSensorRsp.setSequence(sequence);
//        subUpdateSensorRsp.setDeviceId(deviceId);
//        mqttClient.publish(topic + "_rsp", JSON.toJSONString(subUpdateSensorRsp).getBytes(), 2, false);
//    }
//
//    private void processFreePostingOld(String topic, MqttClient mqttClient, String deviceId, String deviceInfo, int sequence) throws MqttException {
//        log.info("接收到隨意貼信息:{}", deviceInfo);
//        FreePosting freePosting = JSONObject.parseObject(deviceInfo, FreePosting.class);
////        FreePosting freePostingCache = (FreePosting) redisTemplate.opsForHash().get(RedisConstants.MqttDevice.DEVICE, deviceId);
//        redisTemplate.opsForHash().put(RedisConstants.MqttDevice.DEVICE, deviceId, freePosting);
//        SubUpdateSensorRsp subUpdateSensorRsp = new SubUpdateSensorRsp();
//        subUpdateSensorRsp.setError(0);
//        subUpdateSensorRsp.setSequence(sequence);
//        subUpdateSensorRsp.setDeviceId(deviceId);
//        mqttClient.publish(topic + "_rsp", JSON.toJSONString(subUpdateSensorRsp).getBytes(), 2, false);
//    }
//
//    //                    if (device1.getDeviceTypeItemId() == 18) {
////                        log.info("设备ID：{}", deviceId);
////                        JSONObject params = deviceInfo.getJSONObject("params");
////                        if (ObjectUtil.isNotEmpty(params)) {
////                            Integer motion = params.getInteger("motion");
////                            Integer battery = params.getInteger("battery");
////                            if (ObjectUtil.isNotEmpty(battery)) {
////                                log.info("人体探测雷达电量：{}", battery);
////                            }
////                            if (ObjectUtil.isNotEmpty(motion)) {
////                                if (motion == 1) {
////                                    log.info("人体探测雷达探测结果：有人");
////                                } else {
////                                    log.info("人体探测雷达探测结果：无人");
////                                }
////                                SubUpdateSensorRsp subUpdateSensorRsp = new SubUpdateSensorRsp();
////                                subUpdateSensorRsp.setError(0);
////                                subUpdateSensorRsp.setSequence(sequence);
////                                subUpdateSensorRsp.setDeviceId(deviceId);
////                                mqttClient.publish(topic + "_rsp", JSON.toJSONString(subUpdateSensorRsp).getBytes(), 2, false);
////                            }
////                        }
////                    }
//    private void processPlugOld(String topic, MqttClient mqttClient, String deviceId, String deviceInfo, int sequence) throws MqttException {
//        log.info("接收到插座信息：{}", deviceInfo);
//        Plug plug = JSONObject.parseObject(deviceInfo, Plug.class);
//        Plug plugCache = (Plug) redisTemplate.opsForHash().get(RedisConstants.MqttDevice.DEVICE, deviceId);
//        if (ObjectUtil.isEmpty(plugCache)) {
//            redisTemplate.opsForHash().put(RedisConstants.MqttDevice.DEVICE, deviceId, plug);
//        } else {
//            //校验开关及各属性
//            List<Switch> newSwitches = plug.getParams().getSwitches();
//            if (ObjectUtils.isNotEmpty(newSwitches)) {
//                //说明这次传入的是开关的数据
//                plugCache.getParams().setSwitches(newSwitches);
//                redisTemplate.opsForHash().put(RedisConstants.MqttDevice.DEVICE, deviceId, plugCache);
//            } else {
//                //说明是除了开关之外的参数
//                if (plug.getParams().getActivePowerA() != null) {
//                    plugCache.getParams().setActivePowerA(plug.getParams().getActivePowerA());
//                } else if (plug.getParams().getActivePowerB() != null) {
//                    plugCache.getParams().setActivePowerB(plug.getParams().getActivePowerB());
//                } else if (plug.getParams().getActivePowerC() != null) {
//                    plugCache.getParams().setActivePowerC(plug.getParams().getActivePowerC());
//                } else if (plug.getParams().getElectricalEnergy() != null) {
//                    plugCache.getParams().setElectricalEnergy(plug.getParams().getElectricalEnergy());
//                } else if (plug.getParams().getRmsCurrentA() != null) {
//                    plugCache.getParams().setRmsCurrentA(plug.getParams().getRmsCurrentA());
//                } else if (plug.getParams().getRmsCurrentB() != null) {
//                    plugCache.getParams().setRmsCurrentB(plug.getParams().getRmsCurrentB());
//                } else if (plug.getParams().getRmsCurrentC() != null) {
//                    plugCache.getParams().setRmsCurrentC(plug.getParams().getRmsCurrentC());
//                } else if (plug.getParams().getRmsVoltageA() != null) {
//                    plugCache.getParams().setRmsVoltageA(plug.getParams().getRmsVoltageA());
//                } else if (plug.getParams().getRmsVoltageB() != null) {
//                    plugCache.getParams().setRmsVoltageB(plug.getParams().getRmsVoltageB());
//                } else if (plug.getParams().getRmsVoltageC() != null) {
//                    plugCache.getParams().setRmsVoltageC(plug.getParams().getRmsVoltageC());
//                }
//                plug.setParams(plugCache.getParams());
//                redisTemplate.opsForHash().put(RedisConstants.MqttDevice.DEVICE, deviceId, plug);
//            }
//        }
//        SubUpdateSensorRsp subUpdateSensorRsp = new SubUpdateSensorRsp();
//        subUpdateSensorRsp.setError(0);
//        subUpdateSensorRsp.setSequence(sequence);
//        subUpdateSensorRsp.setDeviceId(deviceId);
//        mqttClient.publish(topic + "_rsp", JSON.toJSONString(subUpdateSensorRsp).getBytes(), 2, false);
//    }
//
//    private void processSensorOld(String topic, MqttClient mqttClient, String deviceId, String deviceInfo, int sequence) throws MqttException {
//        log.info("接收到传感器信息{}", deviceInfo);
//        Sensor sensor = JSONObject.parseObject(deviceInfo, Sensor.class);
//        Sensor sensorCache = (Sensor) redisTemplate.opsForHash().get(RedisConstants.MqttDevice.DEVICE, deviceId);
//        if (ObjectUtil.isEmpty(sensorCache)) {
//            redisTemplate.opsForHash().put(RedisConstants.MqttDevice.DEVICE, deviceId, sensor);
//            //存儲到
//        } else {
//            //mqtt返回数据
//            SensorParams newParams = sensor.getParams();
//            //缓存数据
//            SensorParams existingParams = sensorCache.getParams();
//            //对mqtt返回属性判断有则用mqtt的 没有仍然用缓存的
//            if (newParams.getMotion() != null) {
//                existingParams.setMotion(newParams.getMotion());
//            } else if (newParams.getLock() != null) {
//                existingParams.setLock(newParams.getLock());
//            } else if (newParams.getWater() != null) {
//                existingParams.setWater(newParams.getWater());
//            } else if (newParams.getKey() != null) {
//                existingParams.setKey(newParams.getKey());
//            } else if (newParams.getSmoke() != null) {
//                existingParams.setSmoke(newParams.getSmoke());
//            } else if (newParams.getTemperature() != null) {
//                existingParams.setTemperature(String.valueOf(Double.parseDouble(newParams.getTemperature()) / 100));
//            } else if (newParams.getHumidity() != null) {
//                existingParams.setHumidity(String.valueOf(Double.parseDouble(newParams.getHumidity()) / 100));
//            } else if (newParams.getBattery() != null) {
//                existingParams.setBattery(newParams.getBattery());
//            } else if (newParams.getEmergency() != null) {
//                existingParams.setEmergency(newParams.getEmergency());
//            } else if (newParams.getIlluminance() != null) {
//                existingParams.setIlluminance(String.valueOf(Double.parseDouble(newParams.getIlluminance()) / 100));
//            }
//            sensor.setParams(existingParams);
//            redisTemplate.opsForHash().put(RedisConstants.MqttDevice.DEVICE, deviceId, sensor);
//        }
//        SubUpdateSensorRsp subUpdateSensorRsp = new SubUpdateSensorRsp();
//        subUpdateSensorRsp.setError(0);
//        subUpdateSensorRsp.setSequence(sequence);
//        subUpdateSensorRsp.setDeviceId(deviceId);
//        mqttClient.publish(topic + "_rsp", JSON.toJSONString(subUpdateSensorRsp).getBytes(), 2, false);
//    }

    @Override
    public HandlerType getType() {
        return HandlerType.SUB_UPDATE;
    }
}
