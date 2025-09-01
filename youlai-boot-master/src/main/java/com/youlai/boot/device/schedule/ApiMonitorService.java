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
 * 设备状态监控服务
 * 定时向设备发送心跳命令，检测设备在线状态
 *
 * @author way
 * @since 2025-06-17
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiMonitorService {

    // ZigBee网关设备心跳监控映射表
    public static ConcurrentHashMap<String, Device> deviceRequestTimeMap = new ConcurrentHashMap<>();

    // MQTT独立设备心跳监控映射表
    public static ConcurrentHashMap<String, Device> deviceMqttRequestTimeMap = new ConcurrentHashMap<>();

    private final DeviceService deviceService;
    private final MqttProducer mqttProducer;

    /**
     * 初始化方法，在Bean创建完成后执行
     * 加载所有网关设备和MQTT设备到监控映射表中
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化设备监控服务");
        mapInit();
        log.info("设备监控服务初始化完成，网关设备数量: {}，MQTT设备数量: {}",
                deviceRequestTimeMap.size(), deviceMqttRequestTimeMap.size());
    }

    /**
     * 初始化设备映射表
     * 1. 加载所有网关设备到deviceRequestTimeMap
     * 2. 加载所有MQTT设备到deviceMqttRequestTimeMap，并设置初始状态为离线
     */
    private void mapInit() {
        log.info("开始加载网关设备列表");
        List<Device> devicesList = deviceService.getGateway();
        for (Device device : devicesList) {
            device.setDeviceLastDate(new Date());
            deviceRequestTimeMap.put(device.getDeviceCode(), device);
        }
        log.info("加载网关设备完成，数量: {}", devicesList.size());

        log.info("开始加载MQTT设备列表");
        List<Device> mqttDevicesList = deviceService.listMqttDevices();
        for (Device device : mqttDevicesList) {
            device.setStatus(0); // 0表示离线状态
            deviceService.updateById(device);
            deviceMqttRequestTimeMap.put(device.getDeviceCode(), device);
        }
        log.info("加载MQTT设备完成，数量: {}", mqttDevicesList.size());
    }

    /**
     * 定时任务：向ZigBee网关设备发送心跳命令
     * 每45秒执行一次，发送backupCoordinator命令检测网关设备在线状态
     */
    @Scheduled(fixedRate = 45000)
    public void offLine() {
        log.info("开始执行ZigBee网关设备心跳检测任务，设备数量: {}", deviceRequestTimeMap.size());

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, Device> entry : deviceRequestTimeMap.entrySet()) {
            String deviceCode = entry.getKey();

            try {
                log.info("向网关设备 {} 发送心跳命令", deviceCode);

                // 构造心跳检测命令
                HashMap<String, Object> backupCommand = new HashMap<>();
                backupCommand.put("sequence", (int) System.currentTimeMillis());
                backupCommand.put("cmd", "backupCoordinator");
                backupCommand.put("params", "");

                // 发送MQTT消息到网关设备
                String topic = "/zbgw/" + deviceCode + "/manage";
                mqttProducer.send(topic, 0, false, JSON.toJSONString(backupCommand));

                successCount++;
                log.info("成功向网关设备 {} 发送心跳命令", deviceCode);

            } catch (MqttException e) {
                failCount++;
                log.error("向网关设备 {} 发送心跳命令失败", deviceCode, e);
            } catch (Exception e) {
                failCount++;
                log.error("处理网关设备 {} 心跳检测时发生未知错误", deviceCode, e);
            }
        }

        log.info("ZigBee网关设备心跳检测任务执行完成，成功: {}，失败: {}", successCount, failCount);
    }

    /**
     * 定时任务：向MQTT独立设备发送状态查询命令
     * 每45秒执行一次，发送STATUS命令检测MQTT设备在线状态
     */
    @Scheduled(fixedRate = 45000)
    public void demo() {
        log.info("开始执行MQTT设备状态查询任务，设备数量: {}", deviceMqttRequestTimeMap.size());

        int successCount = 0;
        int failCount = 0;

        // 统一向MQTT设备发送状态查询命令
        for (Map.Entry<String, Device> entry : deviceMqttRequestTimeMap.entrySet()) {
            String deviceCode = entry.getKey();
            Device device = entry.getValue();

            try {
                log.info("向MQTT设备 {} 发送状态查询命令", deviceCode);

                // 更新设备最后通信时间
                device.setDeviceLastDate(new Date());

                // 构造状态查询命令主题和消息
                String topic = "cmnd/" + deviceCode + "/STATUS";

                // 发送MQTT消息到设备
                mqttProducer.send(topic, 0, false, "8");

                // 更新映射表中的设备信息
                deviceMqttRequestTimeMap.put(deviceCode, device);

                successCount++;
                log.debug("成功向MQTT设备 {} 发送状态查询命令", deviceCode);

            } catch (MqttException e) {
                failCount++;
                log.error("向MQTT设备 {} 发送状态查询命令失败", deviceCode, e);
            } catch (Exception e) {
                failCount++;
                log.error("处理MQTT设备 {} 状态查询时发生未知错误", deviceCode, e);
            }
        }

        log.info("MQTT设备状态查询任务执行完成，成功: {}，失败: {}", successCount, failCount);
    }
}
