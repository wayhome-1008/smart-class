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
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.youlai.boot.common.util.JsonUtils.mergeJson;
import static com.youlai.boot.common.util.MacUtils.getCodeByTopic;

/**
 *@Author: way
 *@CreateTime: 2025-08-29  16:43
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AirSwitchPowerHandler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final InfluxDBProperties influxProperties;
    private final InfluxDBClient influxDBClient;
    private final SceneExecuteService sceneExecuteService;
    private final SceneService sceneService;
    private final AlertRuleEngine alertRuleEngine;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException, JsonProcessingException {
        // 1. 转换消息为JSON

        String deviceCode = getCodeByTopic(topic);
        if (deviceCode.contains("SmartLife")) {
            // 2. 获取设备信息（缓存优先）
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (ObjectUtils.isNotEmpty(device)) {
                JsonNode deviceInfo = device.getDeviceInfo();
                if (ObjectUtils.isNotEmpty(deviceInfo)) {
                    ObjectNode metrics = JsonNodeFactory.instance.objectNode();
                    metrics.put("voltage", deviceInfo.get("voltage").asInt());
                    metrics.put("current", deviceInfo.get("current").asInt());
                    metrics.put("power", deviceInfo.get("power").asInt());
                    metrics.put("total", deviceInfo.get("total").asDouble());
                    metrics.put("switch1", jsonMsg);
                    metrics.put("count", 1);
                    //场景
                    List<Scene> scenesByDeviceId = sceneService.getScenesByDeviceCode(device.getDeviceCode());
                    for (Scene scene : scenesByDeviceId) {
                        sceneExecuteService.executeScene(scene, device, mqttClient, metrics);
                    }
                    //接受得数据与旧数据合并
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
                    //创建influx数据
                    InfluxMqttPlug influxPlug = new InfluxMqttPlug();
                    influxPlug.setCategoryId(device.getCategoryId().toString());
                    //tag为设备编号
                    influxPlug.setDeviceCode(device.getDeviceCode());
                    influxPlug.setSwitchState(metrics.get("switch1").asText());
                    //tag为房间id
                    influxPlug.setRoomId(device.getDeviceRoom().toString());
                    influxPlug.setDeviceType(String.valueOf(device.getDeviceTypeId()));
                    //处理插座数据
                    //电压
                    influxPlug.setVoltage(metrics.get("voltage").asDouble());
                    //电流
                    influxPlug.setCurrent(metrics.get("current").asDouble());
                    //功率
                    influxPlug.setPower((int) metrics.get("power").asDouble());
                    //总用电量
                    influxPlug.setTotal(metrics.get("total").asDouble());
                    log.info("插座数据:{}", influxPlug);
                    if (device.getIsMaster() == 1) {
                        influxDBClient.getWriteApiBlocking().writeMeasurement(influxProperties.getBucket(), influxProperties.getOrg(), WritePrecision.MS, influxPlug);
                    }
                }
                // 更新设备信息到缓存
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
            }
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.POWER;
    }
}
