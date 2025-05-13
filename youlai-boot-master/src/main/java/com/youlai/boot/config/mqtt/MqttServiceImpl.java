//package com.youlai.boot.config.mqtt;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.eclipse.paho.client.mqttv3.MqttClient;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
///**
// * 消息分发处理器
// */
//@Component
//@Slf4j
//public class MqttServiceImpl implements MqttService {
//    @Autowired
//    private MsgHandlerContext msgHandlerContext;
//
//    @Override
//    public void processMessage(String mac,String topic, MqttMessage message, MqttClient mqttClient) {
//        String msgContent = new String(message.getPayload());
//        log.info("接收到主题{}的消息{}:", topic, msgContent);
//        try {
//            MsgHandler msgHandler = msgHandlerContext.getMsgHandler(mac);
//            if (msgHandler == null) {
//                log.error("没有找到对应的消息处理器,topic: " + topic);
//                return;
//            } else {
//                log.info("找到对应的消息处理器,topic: " + topic);
//                msgHandler.process(msgContent,mqttClient); //执行
//            }
//        } catch (IOException e) {
//            log.error("process msg error,msg is: " + msgContent, e);
//        }
//    }
//}
