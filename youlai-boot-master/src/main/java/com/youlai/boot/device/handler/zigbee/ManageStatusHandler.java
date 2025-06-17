package com.youlai.boot.device.handler.zigbee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.youlai.boot.common.util.MacUtils.extractFromTopic;
import static com.youlai.boot.device.schedule.ApiMonitorService.deviceRequestTimeMap;

/**
 *@Author: way
 *@CreateTime: 2025-06-17  16:20
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ManageStatusHandler implements MsgHandler {
    private final DeviceService deviceService;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException, JsonProcessingException {
        //说明在线
        String deviceCode = extractFromTopic(topic);
        //从map获取
        Device device = deviceRequestTimeMap.get(deviceCode);
        if (ObjectUtils.isNotEmpty(device)) {
            log.info("设备{}已上线", device.getDeviceCode());
            //重置 时间
            device.setDeviceLastDate(new Date());
            deviceRequestTimeMap.put(device.getDeviceCode(), device);
            if (device.getStatus() == 3) {
                //说明重新上线
                device.setStatus(1);
                deviceService.updateById(device);
                log.info("网关设备{}已重新上线", device.getDeviceCode());
                //todo 子设备需要在updateHandler里处理状态
            }
        }
        //同时查询其余map中得时间是否过一分钟
        for (Map.Entry<String, Device> stringWashDeviceEntry : deviceRequestTimeMap.entrySet()) {
            if (stringWashDeviceEntry.getValue().getDeviceLastDate().getTime() < System.currentTimeMillis() - 40000) {
                log.info("网关设备{}已离线", stringWashDeviceEntry.getValue().getDeviceCode());
                //说明离线 则网关及子设备需设置离线
                Device updateStatus = new Device();
                updateStatus.setStatus(3);
                updateStatus.setId(stringWashDeviceEntry.getValue().getId());
                deviceService.updateById(updateStatus);
                //同时把离线得网关put到map中
                deviceRequestTimeMap.put(stringWashDeviceEntry.getValue().getDeviceCode(), updateStatus);
                //查子设备
                List<Device> subDevices = deviceService.listGatewaySubDevices(stringWashDeviceEntry.getValue().getId());
                for (Device subDevice : subDevices) {
                    log.info("子设备{}已离线", subDevice.getDeviceCode());
                    subDevice.setStatus(3);
                    deviceService.updateById(subDevice);
                }
            }
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.MANAGE_RSP;
    }
}
