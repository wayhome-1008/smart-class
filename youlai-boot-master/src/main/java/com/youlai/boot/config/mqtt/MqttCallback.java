package com.youlai.boot.config.mqtt;

import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.device.factory.MsgHandlerFactory;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.youlai.boot.config.mqtt.TopicConfig.BASE_TOPIC;
import static com.youlai.boot.config.mqtt.TopicConfig.TOPIC_LIST;

@Component
@Slf4j
public class MqttCallback implements MqttCallbackExtended {
    @Autowired
    private MsgHandlerFactory factory;
    @Autowired
    private DeviceService deviceService;

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
        log.info("【同一消息接收器:接收到主题{}的消息{}】", topic, message.toString());
        String finalTopic = MacUtils.removeZbgwMacPart(topic);
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
                default:
                    break;
            }
            MsgHandler handler = factory.getHandler(type);
            if (handler != null) {
                //注册消息处理
                String parseMessage = new String(message.getPayload());
                handler.process(topic, parseMessage, mqttClient);
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
//        log.info("消息成功发布:{}", iMqttDeliveryToken.isComplete());
    }

    /**
     * @description: 读取配置文件并自动订阅topic
     * @author: way
     * @date: 2024/7/22 15:00
     **/
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        //获取设备
        List<Device> deviceList = deviceService.getDeviceList();
        if (ObjectUtils.isNotEmpty(deviceList)) {
            try {
                //根据设备MAC转换为topic
                for (Device device : deviceList) {
                    if (device.getDeviceTypeId() == 1){
                        String deviceMac = device.getDeviceMac();
                        deviceMac = deviceMac.replace(":", "");
                        for (String consumerTopic : TOPIC_LIST) {
                            mqttClient.subscribe(BASE_TOPIC + deviceMac + consumerTopic, 2);
                            log.info("订阅主题:{}", BASE_TOPIC + deviceMac + consumerTopic);
                        }
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

    private MqttClient mqttClient;

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }
}