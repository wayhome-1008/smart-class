package com.youlai.boot.device.schedule;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.youlai.boot.common.util.MacUtils.extractFromTopic;

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
            deviceRequestTimeMap.put(device.getDeviceCode(), device);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void offLine() {
        for (Map.Entry<String, Device> stringWashDeviceEntry : deviceRequestTimeMap.entrySet()) {
            //定时去发manage
            HashMap<String, Object> backup = new HashMap<>();
            backup.put("sequence", (int) System.currentTimeMillis());
            backup.put("cmd", "backupCoordinator");
            backup.put("params", "");
            try {
                String topic = "/zbgw/" + stringWashDeviceEntry.getValue().getDeviceCode() + "/manage";
                log.info("发送主题{},内容{}", topic, backup);
                mqttProducer.send(topic, 0, false, JSON.toJSONString(backup));
            } catch (MqttException e) {
                log.error("发失败啦 ~~~~~~~", e);
            }
        }
    }
}
