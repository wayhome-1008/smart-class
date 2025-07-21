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
import com.youlai.boot.device.topic.HandlerType;
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
            ObjectNode lightStatus = JsonNodeFactory.instance.objectNode();
            Iterator<String> fieldNames = jsonNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (fieldName.startsWith("POWER")) {
                    lightCount++;
                    String status = jsonNode.get(fieldName).asText();
                    lightStatus.put(fieldName, status);
                    log.debug("灯光路数 {} 状态: {}", fieldName, status);
                }
            }
            lightStatus.put("count", lightCount);
            // 4. 更新设备信息
            if (ObjectUtils.isNotEmpty(device)) {
                JsonNode mergedInfo = mergeJson(device.getDeviceInfo(), lightStatus);
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
                    ObjectNode newData = JsonNodeFactory.instance.objectNode();
                    JsonNode data = device.getDeviceInfo().get("DHT11");
                    //温度
                    if (data.has("Temperature")) {
                        newData.put("temperature", data.get("Temperature").asDouble());
                    }
                    //湿度
                    if (data.has("Humidity")) {
                        newData.put("humidity", data.get("Humidity").asDouble());
                    }
                    mergeJson(device.getDeviceInfo(), newData);
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
            //topic是code 唯一的
            //截取code
            String deviceCode = getCodeByTopic(topic);
            //从设备缓存获取看是否存在
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (ObjectUtils.isEmpty(device)) {
                device = deviceService.getByCode(deviceCode);
            }
            if (ObjectUtils.isNotEmpty(device)) {
                JsonNode jsonNode = stringToJsonNode(jsonMsg);
                JsonNode mergeJson = mergeJson(device.getDeviceInfo(), jsonNode);
                device.setDeviceInfo(mergeJson);
                Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
                if (ObjectUtils.isNotEmpty(deviceCache)) {
                    device.setStatus(1);
                    redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
                } else {
                    device.setStatus(1);
                }
                JsonNode deviceInfo = deviceCache.getDeviceInfo();
                //创建influx数据
                InfluxSensor point = new InfluxSensor();
                //tag为设备编号
                point.setDeviceCode(deviceCache.getDeviceCode());
                //tag为房间编号
                point.setRoomId(deviceCache.getDeviceRoom().toString());
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
                if (deviceInfo.has("BH1750")) {
                    JsonNode light = deviceInfo.get("BH1750");
                    //亮度
                    if (light.has("Illuminance")) {
                        point.setIlluminance(light.get("Illuminance").asDouble());
                    }
                }
                //人
                if (deviceInfo.has("LD2402")) {
                    JsonNode person = deviceInfo.get("LD2402");
                    //亮度
                    if (person.has("Distance")) {
                        double distance = person.get("Distance").asDouble();
                        if (distance > 0) {
                            point.setMotion(1);
                        } else {
                            point.setMotion(0);
                        }
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

    @Override
    public HandlerType getType() {
        return HandlerType.STATE;
    }
}
