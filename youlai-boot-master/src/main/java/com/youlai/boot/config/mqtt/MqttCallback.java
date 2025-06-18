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
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.youlai.boot.common.util.MacUtils.getCodeByTopic;
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

    /**
     * 接收mqtt服务端消息
     * 进行业务处理，业务处理完成后，手动确认消息
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws MqttException {
        //返回消息统一在这里
        log.info("【接收到主题{}的消息{}】", topic, message.toString());
        String finalTopic = "";
        //zbgw
        if (topic.startsWith("/zbgw")) {
            finalTopic = MacUtils.removeZbgwMacPart(topic);
        }
        //tele
        if (topic.startsWith("tele")) {
            //从缓存去设备
            String deviceCode = getCodeByTopic(topic);
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (device == null) {
                device = deviceService.getByCode(deviceCode);
            }
            if (device != null) {
                //温湿度传感器
                if (device.getDeviceTypeId() == 2) {
                    finalTopic = "/SENSOR";
                }
                //灯光
                if (device.getDeviceTypeId() == 8) {
                    finalTopic = "/LIGHT";
                }
                //三合一传感器
                if (device.getDeviceTypeId() == 9) {
                    finalTopic = "/SENSOR3ON1";
                }
            }
        }
        if (topic.startsWith("stat")) {
            //从缓存去设备
            String deviceCode = getCodeByTopic(topic);
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (device == null) {
                device = deviceService.getByCode(deviceCode);
            }
            if (device != null) {
                //灯光
                if (device.getDeviceTypeId() == 8) {
                    finalTopic = "/RESULT";
                }
            }
        }
        if (StringUtils.isNotEmpty(finalTopic)) {
            HandlerType type = null;
            switch (finalTopic) {
                case "/register":
                    type = HandlerType.REGISTER;
                    break;
                case "/report_subdevice":
                    type = HandlerType.REPORT_SUBDEVICE;
                    break;
                case "/add_subdevice":
                    type = HandlerType.ADD_SUB_DEVICE;
                    break;
                case "/event":
                    type = HandlerType.EVENT;
                    break;
                case "/sub/update":
                    type = HandlerType.SUB_UPDATE;
                    break;
                case "/sub/attribute_rsp":
                    type = HandlerType.SUB_ATTRIBUTE_RSP;
                    break;
                case "/sub/control_rsp":
                    type = HandlerType.SUB_CONTROL_RSP;
                    break;
                case "/sub/things_rsp":
                    type = HandlerType.SUB_THINGS_RSP;
                    break;
                case "/manage_rsp":
                    type = HandlerType.MANAGE_RSP;
                    break;
                case "/request":
                    type = HandlerType.REQUEST;
                    break;
                case "/sub/get_rsp":
                    type = HandlerType.SUB_GET_RSP;
                    break;
                case "/ota_rsp":
                    type = HandlerType.OTA_RSP;
                    break;
                case "/SENSOR":
                    type = HandlerType.SENSOR;
                    break;
                case "/LIGHT":
                    type = HandlerType.LIGHT;
                    break;
                case "/SENSOR3ON1":
                    type = HandlerType.SENSOR3ON1;
                    break;
                case "/RESULT":
                    type = HandlerType.RESULT;
                    break;
                default:
                    break;
            }
            MsgHandler handler = factory.getHandler(type);
            if (handler != null) {
                //注册消息处理
                String parseMessage = new String(message.getPayload());
                try {
                    handler.process(topic, parseMessage, mqttClient);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            //        mqttService.processMessage(mac, StrUtils.removeFirstTwoParts(topic), message, mqttClient);
            //处理成功后确认消息
            mqttClient.messageArrivedComplete(message.getId(), message.getQos());
        }

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
//        List<Device> deviceList = deviceService.getDeviceList();
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
                    } else if (device.getCommunicationModeItemId() == 4) {
                        mqttClient.subscribe("tele/" + device.getDeviceCode() + "/SENSOR", 2);
                        mqttClient.subscribe("tele/" + device.getDeviceCode() + "/INFO3", 2);
                        mqttClient.subscribe("tele/" + device.getDeviceCode() + "/STATE", 2);
//                        mqttClient.subscribe("stat/" + device.getDeviceCode() + "/POWER", 2);
                        mqttClient.subscribe("stat/" + device.getDeviceCode() + "/RESULT", 2);
                        mqttClient.subscribe("stat/" + device.getDeviceCode() + "/STATUS8", 2);
                    }
                }
            } catch (MqttException e) {
                log.error("订阅主题失败", e);
            }
        } else {
            log.warn("目前无设备主题可订阅");
        }

//        if ( ObjectUtils.isNotEmpty(consumerTopics)) {
//            for (CarDevice carDevice : carDeviceList) {
//                for (String consumerTopic : consumerTopics) {
//                        log.info("订阅主题:{}", carDevice.getCarDeviceSN() + consumerTopic);
//                }
//            }
//        }
    }

    @Setter
    private MqttClient mqttClient;
}