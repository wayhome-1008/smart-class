package com.youlai.boot.device.handler.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.handler.status.DeviceStatusManager;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import com.youlai.boot.device.model.influx.InfluxSwitch;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import com.youlai.boot.scene.liteFlow.SceneExecuteService;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.service.SceneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.youlai.boot.common.util.JsonUtils.mergeJson;
import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;
import static com.youlai.boot.common.util.MacUtils.getCodeByTopic;

/**
 *@Author: way
 *@CreateTime: 2025-06-18  15:49
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ResultHandler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;
    private final InfluxDBProperties influxProperties;
    private final InfluxDBClient influxDBClient;
    private final SceneExecuteService sceneExecuteService;
    private final SceneService sceneService;
    private final DeviceStatusManager deviceStatusManager;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        try {
            // 1. 转换消息为JSON
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            String deviceCode = getCodeByTopic(topic);
            // 2. 获取设备信息（缓存优先）
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (ObjectUtils.isEmpty(device)) {
                device = deviceService.getByCode(deviceCode);
            }
            // 更新设备在线状态
            deviceStatusManager.updateDeviceOnlineStatus(deviceCode, device, deviceService);
            //计量插座
            if (device.getDeviceTypeId() == 4) {
                plug(jsonNode, device, deviceCode, mqttClient);
            }
            //灯
            if (device.getDeviceTypeId() == 7) {
                light(jsonNode, device, deviceCode, mqttClient);
            }

        } catch (Exception e) {
            log.error("设备 {} 处理失败: {}", topic, e.getMessage(), e);
            throw new RuntimeException("灯光状态处理异常", e);
        }
    }

    private void plug(JsonNode jsonNode, Device device, String deviceCode, MqttClient mqttClient) {
        ObjectNode metrics = JsonNodeFactory.instance.objectNode();
        String power = jsonNode.get("POWER").asText();
        boolean isSwitch = false;
        //此处对开关上次及本次状态进行对比
        if (ObjectUtils.isNotEmpty(device)) {
            if (device.getDeviceInfo().has("switch1")) {
                String lastSwitchState = device.getDeviceInfo().get("switch1").asText();
                if (!Objects.equals(lastSwitchState, power)) {
                    isSwitch = true;
                }
            }
        }
        metrics.put("switch1", power);
        metrics.put("count", 1);
        //场景
        List<Scene> scenesByDeviceId = sceneService.getScenesByDeviceCode(device.getDeviceCode());
        for (Scene scene : scenesByDeviceId) {
            sceneExecuteService.executeScene(scene, device, mqttClient, metrics);
        }
        //创建influx数据
        InfluxMqttPlug influxPlug = new InfluxMqttPlug();
        //tag为设备编号
        influxPlug.setDeviceCode(device.getDeviceCode());
        influxPlug.setCategoryId(device.getCategoryId().toString());
        influxPlug.setRoomId(device.getDeviceRoom().toString());
        influxPlug.setDeviceType(String.valueOf(device.getDeviceTypeId()));
        if (isSwitch) {
            influxPlug.setSwitchState(power);
        }
        influxDBClient.getWriteApiBlocking().writeMeasurement(
                influxProperties.getBucket(),
                influxProperties.getOrg(),
                WritePrecision.MS,
                influxPlug
        );
        JsonNode mergedInfo = mergeJson(device.getDeviceInfo(), metrics);
        device.setDeviceInfo(mergedInfo);
        device.setStatus(1);
        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
    }

    private void light(JsonNode jsonNode, Device device, String deviceCode, MqttClient mqttClient) {
        // 3. 动态处理所有灯光路数
        ObjectNode metrics = JsonNodeFactory.instance.objectNode();
        Iterator<String> fieldNames = jsonNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            //这里这么处理是因为单个传POWER  多个则为POWER1 POWER2---
            if (fieldName.startsWith("POWER")) {
                if (fieldName.equals("POWER")) {
                    String status = jsonNode.get(fieldName).asText();
                    metrics.put("switch1", status);
                    InfluxSwitch influxSwitch = new InfluxSwitch();
                    influxSwitch.setDeviceCode(deviceCode);
                    influxSwitch.setRoomId(device.getDeviceRoom().toString());
                    influxSwitch.setSwitchState(metrics.toString());
                    influxDBClient.getWriteApiBlocking().writeMeasurement(
                            influxProperties.getBucket(),
                            influxProperties.getOrg(),
                            WritePrecision.MS,
                            influxSwitch
                    );
                } else {
                    String status = jsonNode.get(fieldName).asText();
                    metrics.put(fieldName.replace("POWER", "switch"), status);
                    InfluxSwitch influxSwitch = new InfluxSwitch();
                    influxSwitch.setDeviceCode(deviceCode);
                    influxSwitch.setRoomId(device.getDeviceRoom().toString());
                    influxSwitch.setSwitchState(metrics.toString());
                    influxDBClient.getWriteApiBlocking().writeMeasurement(
                            influxProperties.getBucket(),
                            influxProperties.getOrg(),
                            WritePrecision.MS,
                            influxSwitch
                    );
                    log.info("灯光路数 {} 状态: {}", fieldName, status);
                }
            }
        }
        // 4. 更新设备信息
        if (ObjectUtils.isNotEmpty(device)) {
            //场景
            List<Scene> scenesByDeviceId = sceneService.getScenesByDeviceCode(device.getDeviceCode());
            for (Scene scene : scenesByDeviceId) {
                sceneExecuteService.executeScene(scene, device, mqttClient, metrics);
            }
            JsonNode mergedInfo = mergeJson(device.getDeviceInfo(), metrics);
            device.setDeviceInfo(mergedInfo);
            // 双写：Redis缓存 + 数据库
            device.setStatus(1);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
            log.info("设备 {} 灯光状态更新完成", deviceCode);
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.RESULT;
    }
}
