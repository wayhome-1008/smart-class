package com.youlai.boot.device.handler.zigbee;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.SubUpdateSensorRsp;
import com.youlai.boot.device.model.influx.InfluxHumanRadarSensor;
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import com.youlai.boot.device.model.influx.InfluxSensor;
import com.youlai.boot.device.model.influx.InfluxSwitch;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.service.impl.AlertRuleEngine;
import com.youlai.boot.device.topic.HandlerType;
import com.youlai.boot.system.model.entity.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
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
    private final InfluxDBClient influxDBClient;
    private final InfluxDBProperties influxProperties;
    private final AlertRuleEngine alertRuleEngine;

    /**
     * @description: zigBee设备统一分处理方法
     * @author: way
     **/
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
            device.setStatus(1);
            if (ObjectUtil.isNotEmpty(device)) {
                //先校验是否是串口的
                //串口透传设备
                if (device.getCommunicationModeItemId() == 5) {
                    processSerialDevice(topic, mqttClient, device, jsonMsg, sequence);
                    return;
                }
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
                //开关
                if (device.getDeviceTypeId() == 7) {
                    processSwitch(topic, mqttClient, device, jsonMsg, sequence);
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

    private void processSerialDevice(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws MqttException {
        log.info("串口透传设备数据{}", jsonMsg);
        RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
    }

    private void processSocket(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
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
            device.setStatus(1);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
            InfluxSwitch influxSwitch = new InfluxSwitch();
            influxSwitch.setDeviceCode(device.getDeviceCode());
            influxSwitch.setRoomId(String.valueOf(device.getDeviceRoom()));
            influxSwitch.setSwitchState(allSwitchStates.toString());
            influxDBClient.getWriteApiBlocking().writeMeasurement(
                    influxProperties.getBucket(),
                    influxProperties.getOrg(),
                    WritePrecision.MS,
                    influxSwitch
            );
//            deviceService.updateById(device);
            RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
        }

    }

    /**
     * @description: 开关
     * @author: way
     * @date: 2025/5/27 17:36
     **/
    private void processSwitch(String topic, MqttClient mqttClient, Device device, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
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
                device.setStatus(1);
                //todo 将开关状态存influxdb
                InfluxSwitch influxSwitch = new InfluxSwitch();
                influxSwitch.setDeviceCode(device.getDeviceCode());
                influxSwitch.setRoomId(String.valueOf(device.getDeviceRoom()));
                influxSwitch.setSwitchState(allSwitchStates.toString());
                influxDBClient.getWriteApiBlocking().writeMeasurement(
                        influxProperties.getBucket(),
                        influxProperties.getOrg(),
                        WritePrecision.MS,
                        influxSwitch
                );
                log.info("开关状态{}", influxSwitch);
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
//                deviceService.updateById(device);
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
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
                RspMqtt(topic, mqttClient, device.getDeviceCode(), sequence);
            }
    }

    private void processPlug(String topic, MqttClient mqttClient, Device deviceCache, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        JsonNode jsonNode = stringToJsonNode(jsonMsg);
        //获取params
        JsonNode params = jsonNode.get("params");
        ObjectNode metrics = JsonNodeFactory.instance.objectNode();
        //开关状态
        if (params.has("switches")) {
            JsonNode switchesArray = params.get("switches");
            for (JsonNode switchNode : switchesArray) {
                metrics.put("count", 1);
                metrics.put("switch1", Objects.equals(switchNode.get("switch").asText(), "on") ? "ON" : "OFF");
            }
        }
        //电压
        if (params.has("RMS_VoltageA")) {
            metrics.put("voltage", params.get("RMS_VoltageA").asDouble());
        }
        //电流
        if (params.has("RMS_CurrentA")) {
            metrics.put("current", params.get("RMS_CurrentA").asDouble());
        }
        //功率
        if (params.has("activePowerA")) {
            metrics.put("power", params.get("activePowerA").asDouble());
        }
        //总用电量
        if (params.has("electricalEnergy")) {
            metrics.put("total", params.get("electricalEnergy").asDouble());
        }
        //接受得数据与旧数据合并
        JsonNode mergeJson = mergeJson(Optional.of(deviceCache).map(Device::getDeviceInfo).orElse(null), metrics);
        deviceCache.setDeviceInfo(mergeJson);
        //获取旧设备数据信息- 使用deepCopy创建独立拷贝
//        JsonNode oldParams = deviceCache.getDeviceInfo().get("params").deepCopy();
        //获取合并后的params节点
        JsonNode mergeParams = deviceCache.getDeviceInfo();

        //创建influx数据
        InfluxMqttPlug influxPlug = new InfluxMqttPlug();
        //tag为设备编号
        influxPlug.setDeviceCode(deviceCache.getDeviceCode());

        //tag为房间id
        influxPlug.setRoomId(deviceCache.getDeviceRoom().toString());
        influxPlug.setDeviceType(String.valueOf(deviceCache.getDeviceTypeId()));
        //处理插座数据
        if (mergeParams != null) {
            //电压
            if (mergeParams.has("voltage")) {
                influxPlug.setVoltage(mergeParams.get("voltage").asDouble());
            }
            //电流
            if (mergeParams.has("current")) {
                influxPlug.setCurrent(mergeParams.get("current").asDouble());
            }
            //功率
            if (mergeParams.has("power")) {
//                influxPlug.setActivePowerA(mergeParams.get("activePowerA").asDouble());
                influxPlug.setPower((int) mergeParams.get("power").asDouble());
            }
            //总用电量
            if (mergeParams.has("total")) {
                influxPlug.setTotal(mergeParams.get("total").asDouble());
            }
//            if (mergeParams.has("activePowerB") && mergeParams.get("activePowerB").isInt()) {
//                influxPlug.setActivePowerB(mergeParams.get("activePowerB").asInt());
//            }
//            if (mergeParams.has("activePowerC") && mergeParams.get("activePowerC").isInt()) {
//                influxPlug.setActivePowerC(mergeParams.get("activePowerC").asInt());
//            }

//            if (mergeParams.has("RMS_VoltageB") && mergeParams.get("RMS_VoltageB").isInt()) {
//                influxPlug.setRMS_VoltageB(mergeParams.get("RMS_VoltageB").asInt());
//            }
//            if (mergeParams.has("RMS_VoltageC") && mergeParams.get("RMS_VoltageC").isInt()) {
//                influxPlug.setRMS_VoltageC(mergeParams.get("RMS_VoltageC").asInt());
//            }

//            if (mergeParams.has("RMS_CurrentB") && mergeParams.get("RMS_CurrentB").isInt()) {
//                influxPlug.setRMS_CurrentB(mergeParams.get("RMS_CurrentB").asInt());
//            }
//            if (mergeParams.has("RMS_CurrentC") && mergeParams.get("RMS_CurrentC").isInt()) {
//                influxPlug.setRMS_CurrentC(mergeParams.get("RMS_CurrentC").asInt());
//            }
            log.info("插座数据:{}", influxPlug);
            if (deviceCache.getIsMaster() == 1) {
                influxDBClient.getWriteApiBlocking().writeMeasurement(
                        influxProperties.getBucket(),
                        influxProperties.getOrg(),
                        WritePrecision.MS,
                        influxPlug
                );
            }
        }
        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCache.getDeviceCode(), deviceCache);
        RspMqtt(topic, mqttClient, deviceCache.getDeviceCode(), sequence);
    }

    private void processHumanRadarSensor(String topic, MqttClient mqttClient, Device deviceCache, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        //1.字符串转jsonNode
        JsonNode jsonNode = stringToJsonNode(jsonMsg);
        //2.获取params
        JsonNode params = jsonNode.get("params");
        ObjectNode metrics = JsonNodeFactory.instance.objectNode();
        if (params.has("battery")) {
            metrics.put("battery", params.get("battery").asInt());
        }
        if (params.has("Occupancy")) {
            metrics.put("motion", params.get("Occupancy").asInt());
        }
        //接收的数据与旧数据合并
        JsonNode mergeJson = mergeJson(Optional.of(deviceCache).map(Device::getDeviceInfo).orElse(null), metrics);
        deviceCache.setDeviceInfo(mergeJson);
        //创建influx数据
        InfluxHumanRadarSensor point = new InfluxHumanRadarSensor();
        //tag为设备编号
        point.setDeviceCode(deviceCache.getDeviceCode());
        point.setRoomId(deviceCache.getDeviceRoom().toString());
        //处理数据
        if (mergeJson.has("battery")) {
            point.setBattery(mergeJson.get("battery").asInt());
        }
        if (mergeJson.has("motion")) {
            point.setMotion(mergeJson.get("motion").asInt());
        }
        influxDBClient.getWriteApiBlocking().writeMeasurement(
                influxProperties.getBucket(),
                influxProperties.getOrg(),
                WritePrecision.MS,
                point
        );
        log.info("人体传感器数据:{}", point);
        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCache.getDeviceCode(), deviceCache);
        RspMqtt(topic, mqttClient, deviceCache.getDeviceCode(), sequence);

    }

    /**
     * @description: ZigBee温湿度传感器、ZigBee运动传感器motion
     * @author: way
     * @date: 2025/7/22 16:45
     * @param: [topic, mqttClient, deviceCache, jsonMsg, sequence]
     * @return: void
     **/
    private void processSensor(String topic, MqttClient mqttClient, Device deviceCache, String jsonMsg, int sequence) throws JsonProcessingException, MqttException {
        //字符串转jsonNode
        JsonNode jsonNode = stringToJsonNode(jsonMsg);
        //获取params
        JsonNode params = jsonNode.get("params");
        //新建node用于存储接受设备传入信息
        ObjectNode metrics = JsonNodeFactory.instance.objectNode();
        if (params.has("battery")) {
            metrics.put("battery", params.get("battery").asInt());
        }
        if (params.has("temperature")) {
            metrics.put("temperature", params.get("temperature").asDouble() / 100);
        }
        if (params.has("humidity")) {
            metrics.put("humidity", params.get("humidity").asDouble() / 100);
        }
        if (params.has("Illuminance")) {
            metrics.put("illuminance", params.get("Illuminance").asDouble());
        }
        if (params.has("motion")) {
            metrics.put("motion", params.get("motion").asInt());
        }
        //接收得数据于旧数据合并
        JsonNode mergeJson = mergeJson(Optional.of(deviceCache).map(Device::getDeviceInfo).orElse(null), metrics);
        deviceCache.setDeviceInfo(mergeJson);
        //校验警报配置
        AlertRule alertRule = alertRuleEngine.checkAlertConfig(deviceCache.getId(), metrics);
        if (ObjectUtils.isNotEmpty(alertRule)) {
            boolean checkRule = alertRuleEngine.checkRule(alertRule, metrics.get(alertRule.getMetricKey()).asLong());
            //满足条件
            if (checkRule) {
                //创建AlertEvent
                alertRuleEngine.constructAlertEvent(deviceCache, alertRule, metrics);
            }
        }
        //创建influx数据
        InfluxSensor point = new InfluxSensor();
        //tag为设备编号
        point.setDeviceCode(deviceCache.getDeviceCode());
        //tag为房间编号
        point.setRoomId(deviceCache.getDeviceRoom().toString());
        // 处理传感器数据
        if (mergeJson != null) {
            if (mergeJson.has("battery")) {
                point.setBattery(mergeJson.get("battery").asInt());
            }
            if (mergeJson.has("temperature")) {
                point.setTemperature(mergeJson.get("temperature").asDouble());
            }
            if (mergeJson.has("humidity")) {
                point.setHumidity(mergeJson.get("humidity").asDouble());
            }
            if (mergeJson.has("illuminance")) {
                point.setIlluminance(mergeJson.get("illuminance").asDouble());
            }
            if (mergeJson.has("motion")) {
                point.setMotion(mergeJson.get("motion").asInt());
            }
            point.setRoomId(deviceCache.getDeviceRoom().toString());
            influxDBClient.getWriteApiBlocking().writeMeasurement(
                    influxProperties.getBucket(),
                    influxProperties.getOrg(),
                    WritePrecision.MS,
                    point
            );
            redisTemplate.opsForHash().put(
                    RedisConstants.Device.DEVICE,
                    deviceCache.getDeviceCode(),
                    deviceCache
            );
            RspMqtt(topic, mqttClient, deviceCache.getDeviceCode(), sequence);
            log.info("传感器数据:{}", point);
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
