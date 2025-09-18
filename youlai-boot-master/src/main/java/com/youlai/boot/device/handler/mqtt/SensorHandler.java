package com.youlai.boot.device.handler.mqtt;

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
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import com.youlai.boot.device.model.influx.InfluxSensor;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.service.impl.AlertRuleEngine;
import com.youlai.boot.device.topic.HandlerType;
import com.youlai.boot.scene.liteFlow.SceneExecuteService;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.service.SceneService;
import com.youlai.boot.system.model.entity.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.youlai.boot.common.util.JsonUtils.mergeJson;
import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;
import static com.youlai.boot.common.util.MacUtils.getCodeByTopic;

/**
 *@Author: way
 *@CreateTime: 2025-05-23  17:05
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SensorHandler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;
    private final InfluxDBClient influxDBClient;
    private final InfluxDBProperties influxProperties;
    private final AlertRuleEngine alertRuleEngine;
    private final SceneExecuteService sceneExecuteService;
    private final SceneService sceneService;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws JsonProcessingException {
        //从缓存去设备
        String deviceCode = getCodeByTopic(topic);
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (device == null) {
            device = deviceService.getByCode(deviceCode);
        }
        //计量插座
        if (device.getDeviceTypeId() == 4) {
            handlerPlug(jsonMsg, device, mqttClient);
        }
        //温湿度传感器
        if (device.getDeviceTypeId() == 2 || device.getDeviceTypeId() == 9) {
            handlerSensor(topic, jsonMsg, mqttClient);
        }

    }

    private void handlerPlug(String jsonMsg, Device device, MqttClient mqttClient) throws JsonProcessingException {
        JsonNode jsonNode = stringToJsonNode(jsonMsg);
        log.info("MQTT传感器{}", jsonNode);
            /*
       {
  "Time" : "2025-07-21T10:18:16",
  "ENERGY" : {
    "TotalStartTime" : "2025-06-18T11:16:04",
    "Total" : 1.95,
    "Yesterday" : 0.079,
    "Today" : 0.004,
    "Period" : 2,
    "Power" : 20,
    "ApparentPower" : 33,
    "ReactivePower" : 26,
    "Factor" : 0.61,
    "Voltage" : 225,
    "Current" : 0.147
  }
}
             */
        //只获取需要的数据merge
        ObjectNode metrics = JsonNodeFactory.instance.objectNode();
        //接受得数据与旧数据合并)
        metrics.put("power", jsonNode.get("ENERGY").get("Power").asInt());
        metrics.put("voltage", jsonNode.get("ENERGY").get("Voltage").asDouble());
        metrics.put("current", jsonNode.get("ENERGY").get("Current").asDouble());
        metrics.put("total", jsonNode.get("ENERGY").get("Total").asDouble());
        JsonNode mergeJson = mergeJson(Optional.of(device).map(Device::getDeviceInfo).orElse(null), metrics);
        device.setDeviceInfo(mergeJson);
        //校验警报配置
        AlertRule alertRule = alertRuleEngine.checkAlertConfig(device.getId(), metrics);
        if (ObjectUtils.isNotEmpty(alertRule)) {
            boolean checkRule = alertRuleEngine.checkRule(alertRule, metrics.get(alertRule.getMetricKey()).asText());
            //满足条件
            if (checkRule) {
                alertRuleEngine.runningScene(alertRule.getSceneId(), device, mqttClient, metrics);
                //创建AlertEvent
                alertRuleEngine.constructAlertEvent(device, alertRule, metrics);
            }
        }
        device.setStatus(1);
        //创建influx数据
        InfluxMqttPlug influxPlug = new InfluxMqttPlug();
        influxPlug.setCategoryId(device.getCategoryId().toString());
        //tag为设备编号
        influxPlug.setDeviceCode(device.getDeviceCode());
        influxPlug.setRoomId(device.getDeviceRoom().toString());
        influxPlug.setDeviceType(String.valueOf(device.getDeviceTypeId()));
        influxPlug.setTotal(jsonNode.get("ENERGY").get("Total").asDouble());
        influxPlug.setYesterday(jsonNode.get("ENERGY").get("Yesterday").asDouble());
        influxPlug.setToday(jsonNode.get("ENERGY").get("Today").asDouble());
        influxPlug.setPower(jsonNode.get("ENERGY").get("Power").asInt());
        influxPlug.setApparentPower(jsonNode.get("ENERGY").get("ApparentPower").asInt());
        influxPlug.setReactivePower(jsonNode.get("ENERGY").get("ReactivePower").asInt());
        influxPlug.setFactor(jsonNode.get("ENERGY").get("Factor").asDouble());
        influxPlug.setVoltage(jsonNode.get("ENERGY").get("Voltage").asDouble());
        influxPlug.setCurrent(jsonNode.get("ENERGY").get("Current").asDouble());
        if (device.getIsMaster() == 1) {
            influxDBClient.getWriteApiBlocking().writeMeasurement(
                    influxProperties.getBucket(),
                    influxProperties.getOrg(),
                    WritePrecision.MS,
                    influxPlug
            );
        }
        log.info("MQTT计量插座{}", influxPlug);
        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);

    }

    private void handlerSensor(String topic, String jsonMsg, MqttClient mqttClient) {
        try {
            String deviceCode = getCodeByTopic(topic);
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (device == null) {
                device = deviceService.getByCode(deviceCode);
            }
            if (ObjectUtils.isEmpty(device)) {
                return;
            }
            // 1. 处理设备信息更新
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            ObjectNode metrics = extractSensorData(jsonNode);
            log.info("MQTT三合一传感器{}", metrics);
            device.setDeviceInfo(metrics);
            //场景
            List<Scene> scenesByDeviceId = sceneService.getScenesByDeviceCode(device.getDeviceCode());
            for (Scene scene : scenesByDeviceId) {
                sceneExecuteService.executeScene(scene, device, mqttClient, metrics);
            }
            //校验警报配置
            AlertRule alertRule = alertRuleEngine.checkAlertConfig(device.getId(), metrics);
            if (ObjectUtils.isNotEmpty(alertRule)) {
                boolean checkRule = alertRuleEngine.checkRule(alertRule, metrics.get(alertRule.getMetricKey()).asText());
                //满足条件
                if (checkRule) {
                    alertRuleEngine.runningScene(alertRule.getSceneId(), device, mqttClient, metrics);
                    //创建AlertEvent
                    alertRuleEngine.constructAlertEvent(device, alertRule, metrics);
                }
            }
            // 2. 准备InfluxDB数据
            InfluxSensor point = new InfluxSensor();
            point.setDeviceCode(device.getDeviceCode());
            point.setRoomId(device.getDeviceRoom().toString());
            JsonNode deviceInfo = device.getDeviceInfo();
            device.setStatus(1);
            // 更新缓存
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
            // 处理温湿度数据
            if (deviceInfo.has("temperature")) {
                point.setTemperature(deviceInfo.get("temperature").asDouble());
            }
            if (deviceInfo.has("humidity")) {
                point.setHumidity(deviceInfo.get("humidity").asDouble());
            }
            // 处理光照数据
            if (deviceInfo.has("illuminance")) {
                point.setIlluminance(deviceInfo.get("illuminance").asDouble());
            }
            // 处理人体感应数据
            if (deviceInfo.has("motion")) {
                point.setMotion(deviceInfo.get("motion").asDouble() > 0 ? 1 : 0);
            }
            influxDBClient.getWriteApiBlocking().writeMeasurement(
                    influxProperties.getBucket(),
                    influxProperties.getOrg(),
                    WritePrecision.MS,
                    point
            );
        } catch (Exception e) {
            log.error("处理三合一传感器数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("三合一传感器处理异常", e);
        }
    }

    /**
     * 提取关键属性并构建新的JSON对象
     */
    public static ObjectNode extractSensorData(JsonNode rootNode) {
        // 创建新的ObjectNode用于存储结果
        ObjectNode newData = JsonNodeFactory.instance.objectNode();
        // 遍历所有节点提取目标属性
        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode node = entry.getValue();

            // 提取温度（如果尚未提取且当前节点有该字段）
            if (node.has("Temperature")) {
                newData.put("temperature", node.get("Temperature").asDouble());
            }
            // 提取湿度
            if (node.has("Humidity")) {
                newData.put("humidity", node.get("Humidity").asDouble());
            }

            // 提取光照
            if (node.has("Illuminance")) {
                newData.put("illuminance", node.get("Illuminance").asDouble());
            }
            // 提取距离(做特殊处理)
            if (node.has("Distance")) {
                if (node.get("Distance").asDouble() > 0) {
                    newData.put("motion", 1);
                } else {
                    newData.put("motion", 0);
                }
            }
        }
        return newData;
    }

    @Override
    public HandlerType getType() {
        return HandlerType.SENSOR;
    }
}
