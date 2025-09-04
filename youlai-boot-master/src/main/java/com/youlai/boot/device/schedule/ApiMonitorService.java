package com.youlai.boot.device.schedule;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *@Author: way
 *@CreateTime: 2025-06-17  15:43
 *@Description: TODO
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiMonitorService {
    public static ConcurrentHashMap<String, Device> deviceRequestTimeMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Device> deviceMqttRequestTimeMap = new ConcurrentHashMap<>();
    private final DeviceService deviceService;
    private final MqttProducer mqttProducer;

    // 初始化
    @PostConstruct
    public void init() {
        mapInit();
    }

    private void mapInit() {
        List<Device> devicesList = deviceService.getGateway();
        for (Device device : devicesList) {
            device.setDeviceLastDate(new Date());
            device.setStatus(0);
            deviceRequestTimeMap.put(device.getDeviceCode(), device);
        }
        List<Device> mqttDevicesList = deviceService.listMqttDevices();
        for (Device device : mqttDevicesList) {
            deviceMqttRequestTimeMap.put(device.getDeviceCode(), device);
        }
    }

    @Scheduled(fixedRate = 45000)
    public void offLine() {
        for (Map.Entry<String, Device> stringWashDeviceEntry : deviceRequestTimeMap.entrySet()) {
            //定时去发manage
            HashMap<String, Object> backup = new HashMap<>();
            backup.put("sequence", (int) System.currentTimeMillis());
            backup.put("cmd", "backupCoordinator");
            backup.put("params", "");
            try {
                String topic = "/zbgw/" + stringWashDeviceEntry.getValue().getDeviceCode() + "/manage";
                mqttProducer.send(topic, 0, false, JSON.toJSONString(backup));
            } catch (MqttException e) {
                log.error("发失败啦 ~~~~~~~", e);
            }
        }
    }

    @Scheduled(fixedRate = 45000)
    public void demo() {
        //统一发STATE
        for (Map.Entry<String, Device> stringWashDeviceEntry : deviceMqttRequestTimeMap.entrySet()) {
            try {
                stringWashDeviceEntry.getValue().setDeviceLastDate(new Date());
                String topic = "cmnd/" + stringWashDeviceEntry.getValue().getDeviceCode() + "/STATUS";
                mqttProducer.send(topic, 0, false, "8");
                deviceMqttRequestTimeMap.put(stringWashDeviceEntry.getValue().getDeviceCode(), stringWashDeviceEntry.getValue());
            } catch (MqttException e) {
                log.error("发失败啦 ~~~~~~~", e);
            }
        }
    }
}
