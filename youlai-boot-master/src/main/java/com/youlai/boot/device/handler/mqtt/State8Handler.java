package com.youlai.boot.device.handler.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.youlai.boot.common.util.JsonUtils.mergeJson;
import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;
import static com.youlai.boot.common.util.MacUtils.getCodeByTopic;

/**
 *@Author: way
 *@CreateTime: 2025-06-19  10:56
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor

public class State8Handler implements MsgHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;
    private final InfluxDBClient influxDBClient;
    private final InfluxDBProperties influxProperties;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException, JsonProcessingException {
        //从缓存去设备
        String deviceCode = getCodeByTopic(topic);
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (device == null) {
            device = deviceService.getByCode(deviceCode);
        }
        //计量插座
        if (device.getDeviceTypeId() == 4) {
            handlerPlug(jsonMsg, device);
        } else {
            device.setStatus(1);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
//            deviceService.updateById(device);
        }
    }

    private void handlerPlug(String jsonMsg, Device device) throws JsonProcessingException {

        JsonNode jsonNode = stringToJsonNode(jsonMsg);
        //接受得数据与旧数据合并
        JsonNode mergeJson = mergeJson(device.getDeviceInfo(), jsonNode);
        device.setDeviceInfo(mergeJson);
        //创建influx数据
        InfluxMqttPlug influxPlug = new InfluxMqttPlug();
        //tag为设备编号
        influxPlug.setDeviceCode(device.getDeviceCode());
        influxPlug.setRoomId(device.getDeviceRoom().toString());
        influxPlug.setTotal(jsonNode.get("StatusSNS").get("ENERGY").get("Total").asDouble());
        influxPlug.setYesterday(jsonNode.get("StatusSNS").get("ENERGY").get("Yesterday").asDouble());
        influxPlug.setToday(jsonNode.get("StatusSNS").get("ENERGY").get("Today").asDouble());
//            influxPlug.setPeriod(jsonNode.get("ENERGY").get("Period").asDouble());
        influxPlug.setPower(jsonNode.get("StatusSNS").get("ENERGY").get("Power").asInt());
        influxPlug.setApparentPower(jsonNode.get("StatusSNS").get("ENERGY").get("ApparentPower").asInt());
        influxPlug.setReactivePower(jsonNode.get("StatusSNS").get("ENERGY").get("ReactivePower").asInt());
        influxPlug.setFactor(jsonNode.get("StatusSNS").get("ENERGY").get("Factor").asDouble());
        influxPlug.setVoltage(jsonNode.get("StatusSNS").get("ENERGY").get("Voltage").asDouble());
        influxPlug.setCurrent(jsonNode.get("StatusSNS").get("ENERGY").get("Current").asDouble());
        device.setStatus(1);
        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
//        deviceService.updateById(device);
        influxDBClient.getWriteApiBlocking().writeMeasurement(
                influxProperties.getBucket(),
                influxProperties.getOrg(),
                WritePrecision.MS,
                influxPlug
        );
        log.info("插座数据:{}", influxPlug);
    }

    @Override
    public HandlerType getType() {
        return HandlerType.STATUS8;
    }
}
