package com.youlai.boot.device.handler.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.influx.InfluxSensor;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.service.impl.AlertRuleEngine;
import com.youlai.boot.device.topic.HandlerType;
import com.youlai.boot.system.model.entity.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;

import static com.youlai.boot.common.util.JsonUtils.mergeJson;
import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;
import static com.youlai.boot.common.util.MacUtils.getCodeByTopic;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  16:53
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StateHandler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;
    private final InfluxDBClient influxDBClient;
    private final InfluxDBProperties influxProperties;
    private final AlertRuleEngine alertRuleEngine;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        //从缓存去设备
        String deviceCode = getCodeByTopic(topic);
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (device == null) {
            device = deviceService.getByCode(deviceCode);
        }
        //温湿度传感器
        if (device.getDeviceTypeId() == 2) {
            handlerSensor(topic, jsonMsg);
        }
        //灯光
        if (device.getDeviceTypeId() == 8) {
            handlerLight(topic, jsonMsg);
        }
        //三合一传感器
        if (device.getDeviceTypeId() == 9) {
            handlerSensor3On1(topic, jsonMsg);
        }
    }

    private void handlerLight(String topic, String jsonMsg) {
        try {
            // 1. 转换消息为JSON
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            String deviceCode = getCodeByTopic(topic);

            // 2. 获取设备信息（缓存优先）
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            //定义灯光路数
            int lightCount = 0;
            // 3. 动态处理所有灯光路数
            ObjectNode metrics = JsonNodeFactory.instance.objectNode();
            Iterator<String> fieldNames = jsonNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (fieldName.startsWith("POWER")) {
                    lightCount++;
                }
            }
            metrics.put("count", lightCount);
            // 4. 更新设备信息
            if (ObjectUtils.isNotEmpty(device)) {
                JsonNode mergedInfo = mergeJson(device.getDeviceInfo(), metrics);
                device.setDeviceInfo(mergedInfo);
                Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
                if (deviceCache != null) {
                    // 双写：Redis缓存 + 数据库
                    device.setStatus(1);
                    redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
//                    deviceService.updateById(device);
                } else {
                    // 单写：数据库
                    device.setStatus(1);
//                    deviceService.updateById(device);
                }
                log.info("设备 {} 灯光状态更新完成", deviceCode);
            }
        } catch (Exception e) {
            log.error("设备 {} 处理失败: {}", topic, e.getMessage(), e);
            throw new RuntimeException("灯光状态处理异常", e);
        }
    }

    private void handlerSensor(String topic, String jsonMsg) {
        try {
            //topic是code 唯一的
            //截取code
            String deviceCode = getCodeByTopic(topic);
            //从设备缓存获取看是否存在
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (ObjectUtils.isNotEmpty(device)) {
                JsonNode jsonNode = stringToJsonNode(jsonMsg);
                JsonNode mergeJson = mergeJson(device.getDeviceInfo(), jsonNode);
                device.setStatus(1);
                if (device.getDeviceInfo().has("DHT11")) {
                    ObjectNode metrics = JsonNodeFactory.instance.objectNode();
                    JsonNode data = device.getDeviceInfo().get("DHT11");
                    //温度
                    if (data.has("Temperature")) {
                        metrics.put("temperature", data.get("Temperature").asDouble());
                    }
                    //湿度
                    if (data.has("Humidity")) {
                        metrics.put("humidity", data.get("Humidity").asDouble());
                    }
                    mergeJson(device.getDeviceInfo(), metrics);
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
                }
                device.setDeviceInfo(mergeJson);
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
                JsonNode deviceInfo = device.getDeviceInfo();
                //创建influx数据
                InfluxSensor point = new InfluxSensor();
                //tag为设备编号
                point.setDeviceCode(device.getDeviceCode());
                //tag为房间编号
                point.setRoomId(device.getDeviceRoom().toString());
                if (deviceInfo.has("DHT11")) {
                    JsonNode data = deviceInfo.get("DHT11");
                    //温度
                    if (data.has("Temperature")) {
                        point.setTemperature(data.get("Temperature").asDouble());

                    }
                    //湿度
                    if (data.has("Humidity")) {
                        point.setHumidity(data.get("Humidity").asDouble());
                    }
                }
                influxDBClient.getWriteApiBlocking().writeMeasurement(
                        influxProperties.getBucket(),
                        influxProperties.getOrg(),
                        WritePrecision.MS,
                        point
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handlerSensor3On1(String topic, String jsonMsg) {
        try {
            String deviceCode = getCodeByTopic(topic);
            Device device = getDeviceFromCacheOrDB(deviceCode);

            if (device == null) {
                log.warn("Device {} not found", deviceCode);
                return;
            }

            // 1. 处理设备信息更新
            JsonNode jsonNode = stringToJsonNode(jsonMsg);
            updateDeviceInfo(device, jsonNode);

            // 2. 准备InfluxDB数据
            InfluxSensor point = prepareInfluxData(device);

            // 3. 写入InfluxDB
            writeToInfluxDB(point);

        } catch (Exception e) {
            log.error("处理三合一传感器数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("三合一传感器处理异常", e);
        }
    }

    private Device getDeviceFromCacheOrDB(String deviceCode) {
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (device == null) {
            device = deviceService.getByCode(deviceCode);
        }
        return device;
    }

    private void updateDeviceInfo(Device device, JsonNode jsonNode) {
        // 合并原始数据
        JsonNode mergedInfo = mergeJson(device.getDeviceInfo(), jsonNode);
        device.setDeviceInfo(mergedInfo);
        device.setStatus(1);
        // 标准化字段命名
        if (device.getDeviceInfo().has("DHT11")) {
            ObjectNode standardizedData = standardizeFieldNames(device.getDeviceInfo().get("DHT11"));
            mergeJson(device.getDeviceInfo(), standardizedData);
        }

        // 更新缓存
        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
    }

    private ObjectNode standardizeFieldNames(JsonNode data) {
        ObjectNode newData = JsonNodeFactory.instance.objectNode();
        if (data.has("Temperature")) {
            newData.put("temperature", data.get("Temperature").asDouble());
        }
        if (data.has("Humidity")) {
            newData.put("humidity", data.get("Humidity").asDouble());
        }
        if (data.has("Illuminance")) {
            newData.put("illuminance", data.get("Illuminance").asDouble());
        }
        return newData;
    }

    private InfluxSensor prepareInfluxData(Device device) {
        InfluxSensor point = new InfluxSensor();
        point.setDeviceCode(device.getDeviceCode());
        point.setRoomId(device.getDeviceRoom().toString());

        JsonNode deviceInfo = device.getDeviceInfo();

        // 处理温湿度数据
        if (deviceInfo.has("DHT11")) {
            JsonNode data = deviceInfo.get("DHT11");
            if (data.has("Temperature")) {
                point.setTemperature(data.get("Temperature").asDouble());
            }
            if (data.has("Humidity")) {
                point.setHumidity(data.get("Humidity").asDouble());
            }
        }

        // 处理光照数据
        if (deviceInfo.has("BH1750")) {
            JsonNode light = deviceInfo.get("BH1750");
            if (light.has("Illuminance")) {
                point.setIlluminance(light.get("Illuminance").asDouble());
            }
        }

        // 处理人体感应数据
        if (deviceInfo.has("LD2402")) {
            JsonNode person = deviceInfo.get("LD2402");
            if (person.has("Distance")) {
                point.setMotion(person.get("Distance").asDouble() > 0 ? 1 : 0);
            }
        }

        return point;
    }

    private void writeToInfluxDB(InfluxSensor point) {
        influxDBClient.getWriteApiBlocking().writeMeasurement(
                influxProperties.getBucket(),
                influxProperties.getOrg(),
                WritePrecision.MS,
                point
        );
    }


    @Override
    public HandlerType getType() {
        return HandlerType.STATE;
    }
}
