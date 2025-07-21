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
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

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
            handlerPlug(jsonMsg, device);
        }
    }

    private void handlerPlug(String jsonMsg, Device device) throws JsonProcessingException {
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
        //接受得数据与旧数据合并
        metrics.put("power", jsonNode.get("ENERGY").get("Power").asInt());
        metrics.put("voltage", jsonNode.get("ENERGY").get("Voltage").asDouble());
        metrics.put("current", jsonNode.get("ENERGY").get("Current").asDouble());
        metrics.put("total", jsonNode.get("ENERGY").get("Total").asDouble());
        JsonNode mergeJson = mergeJson(Optional.of(device).map(Device::getDeviceInfo).orElse(null), metrics);
        device.setDeviceInfo(mergeJson);
        device.setStatus(1);
//            deviceService.updateById(device);
        //创建influx数据
        InfluxMqttPlug influxPlug = new InfluxMqttPlug();
        //tag为设备编号
        influxPlug.setDeviceCode(device.getDeviceCode());
        influxPlug.setRoomId(device.getDeviceRoom().toString());
        influxPlug.setDeviceType(String.valueOf(device.getDeviceTypeId()));
        influxPlug.setTotal(jsonNode.get("ENERGY").get("Total").asDouble());
        influxPlug.setYesterday(jsonNode.get("ENERGY").get("Yesterday").asDouble());
        influxPlug.setToday(jsonNode.get("ENERGY").get("Today").asDouble());
//            influxPlug.setPeriod(jsonNode.get("ENERGY").get("Period").asDouble());
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


    @Override
    public HandlerType getType() {
        return HandlerType.SENSOR;
    }
}
