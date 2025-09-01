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
import com.youlai.boot.system.model.entity.AlertRule;
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
    private final AlertRuleEngine alertRuleEngine;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException, JsonProcessingException {
        //从缓存去设备
        String deviceCode = getCodeByTopic(topic);
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (device == null) {
            device = deviceService.getByCode(deviceCode);
        }
        log.info("===================设备{},进入了STATUS8===================", device.getDeviceName());
        //计量插座
        if (device.getDeviceTypeId() == 4) {
            handlerPlug(jsonMsg, device);
        } else {
            device.setStatus(1);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
        }
    }

    private void handlerPlug(String jsonMsg, Device device) throws JsonProcessingException {
        JsonNode jsonNode = stringToJsonNode(jsonMsg);
        //只获取需要的数据merge
        ObjectNode metrics = JsonNodeFactory.instance.objectNode();
        //接受得数据与旧数据合并
        metrics.put("power", jsonNode.get("StatusSNS").get("ENERGY").get("Power").asInt());
        metrics.put("voltage", jsonNode.get("StatusSNS").get("ENERGY").get("Voltage").asDouble());
        metrics.put("current", jsonNode.get("StatusSNS").get("ENERGY").get("Current").asDouble());
        metrics.put("total", jsonNode.get("StatusSNS").get("ENERGY").get("Total").asDouble());
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
        influxPlug.setRoomId(device.getDeviceRoom().toString());
        influxPlug.setDeviceType(String.valueOf(device.getDeviceTypeId()));
        influxPlug.setTotal(jsonNode.get("StatusSNS").get("ENERGY").get("Total").asDouble());
        influxPlug.setYesterday(jsonNode.get("StatusSNS").get("ENERGY").get("Yesterday").asDouble());
        influxPlug.setToday(jsonNode.get("StatusSNS").get("ENERGY").get("Today").asDouble());
        influxPlug.setPower(jsonNode.get("StatusSNS").get("ENERGY").get("Power").asInt());
        influxPlug.setApparentPower(jsonNode.get("StatusSNS").get("ENERGY").get("ApparentPower").asInt());
        influxPlug.setReactivePower(jsonNode.get("StatusSNS").get("ENERGY").get("ReactivePower").asInt());
        influxPlug.setFactor(jsonNode.get("StatusSNS").get("ENERGY").get("Factor").asDouble());
        influxPlug.setVoltage(jsonNode.get("StatusSNS").get("ENERGY").get("Voltage").asDouble());
        influxPlug.setCurrent(jsonNode.get("StatusSNS").get("ENERGY").get("Current").asDouble());
        device.setStatus(1);
        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
        if (device.getIsMaster() == 1) {
            influxDBClient.getWriteApiBlocking().writeMeasurement(
                    influxProperties.getBucket(),
                    influxProperties.getOrg(),
                    WritePrecision.MS,
                    influxPlug
            );
        }
        log.info("MQTT插座数据:{}", influxPlug);
    }

    @Override
    public HandlerType getType() {
        return HandlerType.STATUS8;
    }
}
