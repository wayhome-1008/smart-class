package com.youlai.boot.device.handler.zigbee;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.dto.event.DeviceEvent;
import com.youlai.boot.device.model.dto.event.SubDevicesEvent;
import com.youlai.boot.device.model.dto.event.rsp.DeviceEventResult;
import com.youlai.boot.device.model.dto.event.rsp.DeviceStatusEventRsp;
import com.youlai.boot.device.model.dto.event.rsp.SubDevicesResult;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-05-21  17:36
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EventHandler implements MsgHandler {
    private final DeviceService deviceService;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException {
        //目前仅对子设备在线离线做处理
        log.info("[接收到网关转发子设备事件:{}]", jsonMsg);
        DeviceEvent deviceEvent = JSON.parseObject(jsonMsg, DeviceEvent.class);
        //更新子设备状态
        deviceService.updateDeviceStatusByCode(deviceEvent.getParams());
        //组返回数据
        DeviceStatusEventRsp deviceStatusEventRsp = new DeviceStatusEventRsp();
        deviceStatusEventRsp.setSequence(deviceEvent.getSequence());
        deviceStatusEventRsp.setError(0);
        DeviceEventResult deviceEventResult = getDeviceEventResult(deviceEvent);
        deviceStatusEventRsp.setResult(deviceEventResult);
        //发送事件相应
        mqttClient.publish(topic + "_rsp", JSON.toJSONString(deviceStatusEventRsp).getBytes(), 2, false);
    }

    @NotNull
    private static DeviceEventResult getDeviceEventResult(DeviceEvent deviceEvent) {
        DeviceEventResult deviceEventResult = new DeviceEventResult();
        List<SubDevicesEvent> subDevices = deviceEvent.getParams().getSubDevices();
        List<SubDevicesResult> subDevicesResults = new ArrayList<>();
        for (SubDevicesEvent subDevice : subDevices) {
            SubDevicesResult subDevicesResult = new SubDevicesResult();
            subDevicesResult.setDeviceId(subDevice.getDeviceId());
            subDevicesResult.setError(0);
            subDevicesResults.add(subDevicesResult);
        }
        deviceEventResult.setSubDevices(subDevicesResults);
        return deviceEventResult;
    }

    @Override
    public HandlerType getType() {
        return HandlerType.EVENT;
    }
}
