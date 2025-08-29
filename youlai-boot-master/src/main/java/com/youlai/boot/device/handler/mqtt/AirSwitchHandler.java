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
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.youlai.boot.common.util.JsonUtils.mergeJson;
import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;
import static com.youlai.boot.common.util.MacUtils.getCodeByTopic;

/**
 *@Author: way
 *@CreateTime: 2025-08-29  16:01
 *@Description: 空气开关设备消息处理器
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AirSwitchHandler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;
    private final InfluxDBProperties influxProperties;
    private final InfluxDBClient influxDBClient;
    private final SceneExecuteService sceneExecuteService;
    private final SceneService sceneService;
    private final AlertRuleEngine alertRuleEngine;
    // 使用ConcurrentHashMap存储设备最后接收数据的时间
    private static final ConcurrentHashMap<String, Long> deviceLastDataTimeMap = new ConcurrentHashMap<>();

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException, JsonProcessingException {
        // 1. 转换消息为JSON
        JsonNode jsonNode = stringToJsonNode(jsonMsg);
        String deviceCode = getCodeByTopic(topic);
        // 2. 获取设备信息（缓存优先）
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (ObjectUtils.isNotEmpty(device)) {
            // 更新设备最后接收数据的时间
            updateDeviceLastDataTime(deviceCode);

            // 检查并更新设备在线状态
            checkAndUpdateDeviceStatus(device);

            ObjectNode metrics = JsonNodeFactory.instance.objectNode();
            int voltageA = jsonNode.get("voltageA").asInt();
            int current = jsonNode.get("current").asInt();
            int power = jsonNode.get("power").asInt();
            double total = jsonNode.get("electricDegree").asDouble();
            boolean relayState = jsonNode.get("relayState").asBoolean();
            metrics.put("voltage", voltageA);
            metrics.put("current", current);
            metrics.put("power", power);
            metrics.put("total", total);
            metrics.put("switch1", relayState ? "ON" : "OFF");
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
                boolean checkRule = alertRuleEngine.checkRule(alertRule, metrics.get(alertRule.getMetricKey()).asLong());
                //满足条件
                if (checkRule) {
                    //创建AlertEvent
                    alertRuleEngine.constructAlertEvent(device, alertRule, metrics);
                }
            }
            //创建influx数据
            InfluxMqttPlug influxPlug = new InfluxMqttPlug();
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
                influxDBClient.getWriteApiBlocking().writeMeasurement(
                        influxProperties.getBucket(),
                        influxProperties.getOrg(),
                        WritePrecision.MS,
                        influxPlug
                );
            }
            // 更新设备信息到缓存
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.status;
    }

    /**
     * 更新设备最后接收数据的时间
     * @param deviceCode 设备编码
     */
    private void updateDeviceLastDataTime(String deviceCode) {
        deviceLastDataTimeMap.put(deviceCode, Instant.now().toEpochMilli());
    }

    /**
     * 检查并更新设备在线状态
     * @param device 设备对象
     */
    private void checkAndUpdateDeviceStatus(Device device) {
        Long lastDataTime = deviceLastDataTimeMap.get(device.getDeviceCode());

        if (lastDataTime != null) {
            long currentTime = Instant.now().toEpochMilli();
            long timeDiff = currentTime - lastDataTime;

            // 如果超过1分钟没有收到数据，将设备状态设置为离线
            if (timeDiff > 60 * 1000) { // 60秒 = 1分钟
                if (device.getStatus() != 0) { // 0表示离线，1表示在线
                    device.setStatus(0);
                    // 更新数据库中的设备状态
                    deviceService.updateById(device);
                    log.info("设备 {} 超时未接收数据，状态已设置为离线", device.getDeviceCode());
                }
            } else {
                // 如果在1分钟内收到了数据，确保设备状态为在线
                if (device.getStatus() != 1) {
                    device.setStatus(1);
                    // 更新数据库中的设备状态
                    deviceService.updateById(device);
                    log.info("设备 {} 接收到数据，状态已设置为在线", device.getDeviceCode());
                }
            }
        } else {
            // 如果没有记录最后数据时间，初始化为当前时间
            updateDeviceLastDataTime(device.getDeviceCode());
            if (device.getStatus() != 1) {
                device.setStatus(1);
                deviceService.updateById(device);
                log.info("设备 {} 初始化在线状态", device.getDeviceCode());
            }
        }
    }

    /**
     * 定时清理过期的设备时间记录（每10分钟执行一次）
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 10分钟
    public void cleanExpiredDeviceRecords() {
        long currentTime = Instant.now().toEpochMilli();
        long expireTime = 5 * 60 * 1000; // 5分钟过期

        deviceLastDataTimeMap.entrySet().removeIf(entry -> {
            long timeDiff = currentTime - entry.getValue();
            if (timeDiff > expireTime) {
                log.debug("清理过期设备记录: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
