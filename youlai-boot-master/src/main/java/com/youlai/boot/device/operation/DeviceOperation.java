package com.youlai.boot.device.operation;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.model.dto.Control;
import com.youlai.boot.device.model.dto.Switch;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceOperate;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.youlai.boot.device.controller.DeviceOperateController.makeControlParams;

/**
 *@Author: way
 *@CreateTime: 2025-09-11  15:54
 *@Description: TODO
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceOperation {
    private final RedisTemplate<String, Object> redisTemplate;

    //根据场景操作设备
    public void operate(DeviceOperate deviceOperate, String deviceCode, MqttClient mqttClient) throws MqttException {
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        //发送前判断当前状态和目标状态是否一致

        if (ObjectUtils.isEmpty(device)) return;
        if (device.getDeviceTypeId() != 4 && device.getDeviceTypeId() != 7 && device.getDeviceTypeId() != 10 && device.getDeviceTypeId() != 8)
            return;
        //根据通信协议去发送不同协议报文
        switch (CommunicationModeEnum.getNameById(device.getCommunicationModeItemId())) {
            case "ZigBee" ->
//            zigBee
                    zigBeeDevice(device.getDeviceCode(), device.getDeviceGatewayId(), deviceOperate.getOperate(), deviceOperate.getWay(), -1, mqttClient);
            case "WiFi" ->
                //WiFi
                    wifiDevice(device.getDeviceCode(), deviceOperate.getOperate(), deviceOperate.getWay(), -1, mqttClient);
            default -> Result.failed("暂不支持该协议");
        }
    }
    private void zigBeeDevice(String deviceCode, Long deviceGatewayId, @Pattern(regexp = "ON|OFF", message = "错误操作") String operate, String way, Integer count, MqttClient mqttClient) throws MqttException {
        log.info("zigBeeDevice直接出发");
        Control control = makeControl(deviceCode);
        Switch plug = makeSwitch(operate, Integer.parseInt(way) - 1);
        List<Switch> switches = new ArrayList<>();
        switches.add(plug);
        makeControlParams(switches, control);
        mqttClient.publish("/zbgw/" + deviceCode + "/sub/control", JSON.toJSONString(control).getBytes(), 2, false);
    }

    @NotNull
    private static Switch makeSwitch(String operate, int i) {
        Switch plug = new Switch();
        plug.setSwitchStatus(operate.equals("ON") ? "on" : "off");
        plug.setOutlet(i);
        return plug;
    }

    @NotNull
    private static Control makeControl(String deviceCode) {
        Control control = new Control();
        control.setDeviceId(deviceCode);
        control.setSequence((int) System.currentTimeMillis());
        return control;
    }

    private void wifiDevice(String deviceCode, String operate, String way, Integer lightCount, MqttClient mqttClient) {
        //目前能控制的就只有灯的开关
        log.info("正在发送~~~~~~~~~~");
        //判断几路
//        if (lightCount == 1) {
//            try {
//                mqttClient.publish("cmnd/" + deviceCode + "/POWER", operate.getBytes(), 1, false);
//            } catch (MqttException e) {
//                log.error("发送消息失败", e);
//            }
//
//        } else if (way.equals("-1")) {
//            try {
//                for (int i = 1; i <= lightCount; i++) {
//                    mqttClient.publish("cmnd/" + deviceCode + "/POWER" + i, operate.getBytes(), 1, false);
//                }
//            } catch (MqttException e) {
//                log.error("发送消息失败", e);
//            }
//        } else {
        try {
            mqttClient.publish("cmnd/" + deviceCode + "/POWER" + way, operate.getBytes(), 1, false);
            String topic = "cmnd/" + deviceCode + "/POWER" + way;
            String payload = JSON.toJSONString(operate);
            log.info("发送MQTT消息 - 主题: {}, 内容: {}", topic, payload);
        } catch (MqttException e) {
            log.error("发送消息失败", e);
        }
    }
}
