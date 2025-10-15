package com.youlai.boot.device.handler.zigbee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.core.log.LogHelper;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.handler.status.DeviceStatusManager;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.youlai.boot.common.util.MacUtils.extractFromTopic;

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
    private final LogHelper logHelper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceStatusManager deviceStatusManager;
    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) throws MqttException, JsonProcessingException {
        //说明在线
        String deviceCode = extractFromTopic(topic);
        Device gateWay = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (ObjectUtils.isNotEmpty(gateWay)) {
            deviceStatusManager.updateDeviceOnlineStatus(gateWay.getDeviceCode());
        }
//        //从map获取
//        Device device = deviceRequestTimeMap.get(deviceCode);
//        if (ObjectUtils.isNotEmpty(device)) {
//            deviceOnline(device);
//            if (device.getStatus() == 3) {
//                deviceReOnline(device);
//            }
//        }
//        //同时查询其余map中得时间是否过一分钟
//        for (Map.Entry<String, Device> stringWashDeviceEntry : deviceRequestTimeMap.entrySet()) {
//            if (!Objects.equals(stringWashDeviceEntry.getValue().getDeviceCode(), deviceCode)) {
//                if (stringWashDeviceEntry.getValue().getDeviceLastDate().getTime() < System.currentTimeMillis() - 60000) {
//                    deviceOffline(stringWashDeviceEntry, device);
//                }
//            }
//        }
    }

//
//    public void deviceOffline(Map.Entry<String, Device> stringWashDeviceEntry, Device device) {
//        long startTime = System.currentTimeMillis();
//        log.info("{}网关设备已离线", device.getDeviceName());
//        //说明离线 则网关及子设备需设置离线
//        Device updateStatus = new Device();
//        updateStatus.setStatus(3);
//        updateStatus.setId(stringWashDeviceEntry.getValue().getId());
//        deviceService.updateById(updateStatus);
//        //同时把离线得网关put到map中
//        stringWashDeviceEntry.getValue().setStatus(3);
//        deviceRequestTimeMap.put(stringWashDeviceEntry.getValue().getDeviceCode(), stringWashDeviceEntry.getValue());
//        //查子设备
//        List<Device> subDevices = deviceService.listGatewaySubDevices(stringWashDeviceEntry.getValue().getId());
//        for (Device subDevice : subDevices) {
//            log.info("{}网关设备的子设备{}已离线", device.getDeviceName(), subDevice.getDeviceName());
//            subDevice.setStatus(3);
//            deviceService.updateById(subDevice);
//        }
//        logHelper.recordMethodLog(
//                LogModuleEnum.WARNING,
//                "网关设备已离线",
//                "deviceOffline",
//                Map.of(
//                        "gateway", device.getDeviceName(),
//                        "offlineDevice", subDevices
//                ),
//                null,
//                System.currentTimeMillis() - startTime
//        );
//    }
//
//    public void deviceReOnline(Device device) {
//        //说明重新上线
//        device.setStatus(1);
//        deviceService.updateById(device);
//        //重置 时间
//        device.setDeviceLastDate(new Date());
//        deviceRequestTimeMap.put(device.getDeviceCode(), device);
//        log.info("{}网关设备重新上线", device.getDeviceName());
//        //重新上线的话 按理说网关会自动发同步子设备消息 我会在那里处理子设备的状态问题
//    }
//
//    public static void deviceOnline(Device device) {

    ////        log.info("{}网关设备为在线", device.getDeviceName());
//        //重置 时间
//        device.setDeviceLastDate(new Date());
//        deviceRequestTimeMap.put(device.getDeviceCode(), device);
//    }
    @Override
    public HandlerType getType() {
        return HandlerType.MANAGE_RSP;
    }
}
