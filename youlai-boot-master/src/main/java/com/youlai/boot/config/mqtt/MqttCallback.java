package com.youlai.boot.config.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.device.factory.MsgHandlerFactory;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.youlai.boot.config.mqtt.TopicConfig.BASE_TOPIC;
import static com.youlai.boot.config.mqtt.TopicConfig.TOPIC_LIST;

@Component
@Slf4j
public class MqttCallback implements MqttCallbackExtended {
    @Autowired
    private MsgHandlerFactory factory;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void connectionLost(Throwable throwable) {
        log.error("mqtt链接丢失", throwable);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws MqttException {
        log.info("【接收到主题{}的消息{}】", topic, message.toString());
        //获取一条消息则redis+1
        // 原子性递增消息计数
        Long count = redisTemplate.opsForValue().increment(RedisConstants.MessageCount.MESSAGE_COUNT_KEY);

        // 设置过期时间（可选），防止key永久存在
        if (count != null && count == 1) {
            redisTemplate.expire(RedisConstants.MessageCount.MESSAGE_COUNT_KEY, 30, TimeUnit.DAYS);
        }
        // 统一处理路径
        String normalizedTopic = normalizeTopic(topic);
        HandlerType type = determineHandlerType(normalizedTopic);

        if (type != null) {
            MsgHandler handler = factory.getHandler(type);
            if (handler != null) {
                try {
                    handler.process(topic, new String(message.getPayload()), mqttClient);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            mqttClient.messageArrivedComplete(message.getId(), message.getQos());
        }
    }

    // 标准化 topic 路径
    private String normalizeTopic(String topic) {
        if (topic.startsWith("/zbgw")) {
            return MacUtils.removeZbgwMacPart(topic); // 示例: "/zbgw/abc123/manage" -> "/manage"
        } else if (topic.contains("tasmota") || topic.contains("SmartLife")) {
            return MacUtils.getFinalByTopic(topic);   // 示例: "tele/tasmota_abc/STATE" -> "STATE"
        }
        return topic;
    }

    // 统一判断 HandlerType
    private HandlerType determineHandlerType(String normalizedTopic) {
        return switch (normalizedTopic) {
            // 处理 tasmota 相关功能
            case "STATE" -> HandlerType.STATE;
            case "RESULT" -> HandlerType.RESULT;
            case "STATUS8" -> HandlerType.STATUS8;
            case "SENSOR" -> HandlerType.SENSOR;
            case "LIGHT" -> HandlerType.LIGHT;
            case "SENSOR3ON1" -> HandlerType.SENSOR3ON1;
            case "status"-> HandlerType.status;
            case "POWER" -> HandlerType.POWER;
            // 处理通用路径
            case "/register" -> HandlerType.REGISTER;
            case "/report_subdevice" -> HandlerType.REPORT_SUBDEVICE;
            case "/add_subdevice" -> HandlerType.ADD_SUB_DEVICE;
            case "/event" -> HandlerType.EVENT;
            case "/sub/update" -> HandlerType.SUB_UPDATE;
            case "/sub/attribute_rsp" -> HandlerType.SUB_ATTRIBUTE_RSP;
            case "/sub/control_rsp" -> HandlerType.SUB_CONTROL_RSP;
            case "/sub/things_rsp" -> HandlerType.SUB_THINGS_RSP;
            case "/manage_rsp" -> HandlerType.MANAGE_RSP;
            case "/request" -> HandlerType.REQUEST;
            case "/sub/get_rsp" -> HandlerType.SUB_GET_RSP;
            case "/ota_rsp" -> HandlerType.OTA_RSP;
            case "/SENSOR" -> HandlerType.SENSOR;
            case "/LIGHT" -> HandlerType.LIGHT;
            case "/SENSOR3ON1" -> HandlerType.SENSOR3ON1;
            default -> null;
        };
    }

    /**
     * @description: 消息发送完成
     * @author: way
     * @date: 2024/7/22 14:59
     * @param: [iMqttDeliveryToken]
     * @return: void
     **/
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
    }

    /**
     * 提供给ZigBee网关及MQTT独立设备新增时 动态订阅主题 当服务重启时会对全部设备进行订阅
     * @author: way
     * @date: 2025/6/16 9:33
     * @param: [topic]
     **/
    public void subscribeTopic(String topic) {
        try {
            mqttClient.subscribe(topic, 2);
            log.info("动态订阅主题: {}", topic);
        } catch (MqttException e) {
            log.error("动态订阅主题失败", e);
        }
    }

    /**
     * 动态取消订阅主题
     * @author: way
     * @date: 2025/6/16 9:33
     * @param topic 要取消订阅的MQTT主题
     */
    public void unsubscribeTopic(String topic) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.unsubscribe(topic);
                log.info("动态取消订阅主题: {}", topic);
            }
        } catch (MqttException e) {
            log.error("动态取消订阅主题失败", e);
        }
    }

    /**
     * @description: 读取配置文件并自动订阅topic
     * @author: way
     * @date: 2024/7/22 15:00
     **/
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        //获取设备
        List<Device> deviceList = deviceService.list();
        if (ObjectUtils.isNotEmpty(deviceList)) {
            try {
                //根据设备MAC转换为topic
                for (Device device : deviceList) {
                    if (device.getDeviceTypeId() == 1) {
                        String deviceMac = MacUtils.reParseMACAddress(device.getDeviceMac());
                        for (String consumerTopic : TOPIC_LIST) {
                            mqttClient.subscribe(BASE_TOPIC + deviceMac + consumerTopic, 2);
                            log.info("订阅主题:{}", BASE_TOPIC + deviceMac + consumerTopic);
                        }
                        //独立mqtt通信设备
                    } else if (device.getCommunicationModeItemId() == 2) {
                        log.info("订阅主题:{}", "tele/" + device.getDeviceCode() + "/SENSOR");
                        mqttClient.subscribe("tele/" + device.getDeviceCode() + "/SENSOR", 2);
                        log.info("订阅主题:{}", "tele/" + device.getDeviceCode() + "/INFO3");
                        mqttClient.subscribe("tele/" + device.getDeviceCode() + "/INFO3", 2);
                        log.info("订阅主题:{}", "tele/" + device.getDeviceCode() + "/STATE");
                        mqttClient.subscribe("tele/" + device.getDeviceCode() + "/STATE", 2);
//                        mqttClient.subscribe("stat/" + device.getDeviceCode() + "/POWER", 2);
                        log.info("订阅主题:{}", "stat/" + device.getDeviceCode() + "/RESULT");
                        mqttClient.subscribe("stat/" + device.getDeviceCode() + "/RESULT", 2);
                        log.info("订阅主题:{}", "stat/" + device.getDeviceCode() + "/STATUS8");
                        mqttClient.subscribe("stat/" + device.getDeviceCode() + "/STATUS8", 2);
                        log.info("订阅主题:{}", "stat/" + device.getDeviceCode() + "/status");
                        mqttClient.subscribe("stat/" + device.getDeviceCode() + "/status", 2);
                        log.info("订阅主题:{}", "cmnd/" + device.getDeviceCode() + "/POWER");
                        mqttClient.subscribe("cmnd/" + device.getDeviceCode() + "/POWER", 2);
                    }
                }
            } catch (MqttException e) {
                log.error("订阅主题失败", e);
            }
        } else {
            log.warn("目前无设备主题可订阅");
        }
    }

    @Setter
    private MqttClient mqttClient;
}