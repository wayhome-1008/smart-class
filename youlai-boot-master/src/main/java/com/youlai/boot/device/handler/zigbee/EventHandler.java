package com.youlai.boot.device.handler.zigbee;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.core.log.LogHelper;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.dto.event.DeviceEvent;
import com.youlai.boot.device.model.dto.event.SubDevicesEvent;
import com.youlai.boot.device.model.dto.event.rsp.DeviceEventResult;
import com.youlai.boot.device.model.dto.event.rsp.DeviceStatusEventRsp;
import com.youlai.boot.device.model.dto.event.rsp.SubDevicesResult;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final LogHelper logHelper;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException {
        //目前仅对子设备在线离线做处理
//        log.info("[接收到网关转发子设备事件:{}]", jsonMsg);
        DeviceEvent deviceEvent = JSON.parseObject(jsonMsg, DeviceEvent.class);
        if (deviceEvent != null) {
            String event = Optional.of(deviceEvent).map(DeviceEvent::getEvent).orElse(null);
            if (StringUtils.isNotEmpty(event)) {
                if (event.equals("onoffline")) {
                    //更新子设备状态
                    deviceService.updateDeviceStatusByCode(deviceEvent.getParams());
                    //log
                    logHelper.recordMethodLog(LogModuleEnum.WARNING, "网关子设备离线", "子设备离线", deviceEvent, null, 0);
                }
                if (event.equals("leave")) {
                    //说明该设备被重置 按理说我该删除或给警报 先给个状态2吧。。。
                    Device updateDevice = new Device();
                    updateDevice.setStatus(2);
                    deviceService.update(updateDevice, new LambdaQueryWrapper<Device>().eq(Device::getDeviceCode, deviceEvent.getParams().getSubDevices().get(0).getDeviceId()));
                    logHelper.recordMethodLog(LogModuleEnum.WARNING, "网关子设备重置", "子设备重置", deviceEvent, null, 0);

                }
            }
            //组返回数据
            DeviceStatusEventRsp deviceStatusEventRsp = new DeviceStatusEventRsp();
            deviceStatusEventRsp.setSequence(deviceEvent.getSequence());
            deviceStatusEventRsp.setError(0);
            DeviceEventResult deviceEventResult = getDeviceEventResult(deviceEvent);
            deviceStatusEventRsp.setResult(deviceEventResult);
            //发送事件相应
            mqttClient.publish(topic + "_rsp", JSON.toJSONString(deviceStatusEventRsp).getBytes(), 2, false);
        }

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
