package com.youlai.boot.device.handler.status;

import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//
///**
// *@Author: way
// *@CreateTime: 2025-09-01  15:11
// *@Description: 负责跟踪设备的在线状态，自动检测设备离线情况
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DeviceStatusManager {
//    // 使用ConcurrentHashMap存储设备最后接收数据的时间
//    public static final ConcurrentHashMap<String, Device> deviceStatusMap = new ConcurrentHashMap<>();
//
//    // 设备超时时间（毫秒）- 1分钟
//    private static final long DEVICE_TIMEOUT = 5 * 60 * 1000;
//    //当下就给人体传感的超时时间多一些
//    private static final long BODY_SENSOR_TIMEOUT = 10 * 60 * 1000;
//    // 设备记录过期时间（毫秒）- 5分钟
//    private static final long RECORD_EXPIRE_TIME = 5 * 60 * 1000;
//
//    private final RedisTemplate<String, Object> redisTemplate;
//    private final DeviceService deviceService;
//
//    /**
//     * 更新设备在线状态
//     * 当设备发送数据时调用此方法，自动更新设备状态为在线
//     * 并定期检查设备是否超时离线
//     *
//     */
//    public void updateDeviceOnlineStatus(String deviceCode) {
//        // 更新设备最后接收数据的时间
//        updateDeviceLastDataTime(deviceCode);
//
//        // 检查并更新设备在线状态
//        checkAndUpdateAllDeviceStatus();
//    }
//
//    /**
//     * 更新设备最后接收数据的时间
//     */
//    private void updateDeviceLastDataTime(String deviceCode) {
//        Device device = deviceStatusMap.get(deviceCode);
//        if (device == null) {
//            return;
//        }
//        device.setLastOnlineTime(Instant.now().toEpochMilli());
//        deviceStatusMap.put(deviceCode, device);
//    }
//
//    /**
//     * 检查并更新所有设备在线状态
//     */
//    public void checkAndUpdateAllDeviceStatus() {
//        List<Device> devicesToUpdate = new ArrayList<>();
//        for (Map.Entry<String, Device> entry : deviceStatusMap.entrySet()) {
//            String deviceCode = entry.getKey();
//            Long lastDataTime = entry.getValue().getLastOnlineTime();
//            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
//            long currentTime = Instant.now().toEpochMilli();
//            long timeDiff = currentTime - lastDataTime;
//            if (ObjectUtils.isEmpty(device.getStatus())) {
//                continue;
//            }
//            device.setLastOnlineTime(lastDataTime);
//            if (device.getDeviceTypeId() == 6) {
//                if (timeDiff > BODY_SENSOR_TIMEOUT) {
//                    if (device.getStatus() != 0) {
//                        device.setStatus(0);
//                        devicesToUpdate.add(device);
//                        log.info("人体设备 {} 超时未接收数据，状态已设置为离线", deviceCode);
//                    }
//                } else {
//                    if (device.getStatus() != 1) {
//                        device.setStatus(1);
//                        devicesToUpdate.add(device);
//                        log.info("人体设备 {} 接收到数据，状态已设置为在线", deviceCode);
//                    }
//                }
//            } else {
//                if (timeDiff > DEVICE_TIMEOUT) {
//                    if (device.getStatus() != 0) {
//                        device.setStatus(0);
//                        devicesToUpdate.add(device);
//                        log.info("设备 {} 超时未接收数据，状态已设置为离线", deviceCode);
//                    }
//                } else {
//                    if (device.getStatus() != 1) {
//                        device.setStatus(1);
//                        devicesToUpdate.add(device);
//                        log.info("设备 {} 接收到数据，状态已设置为在线", deviceCode);
//                    }
//                }
//            }
//        }
//        // 批量更新数据库中的设备状态
//        if (!devicesToUpdate.isEmpty()) {
//            for (Device deviceUpdate : devicesToUpdate) {
//                log.info("更新设备状态: {}", deviceUpdate.toString());
//                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceUpdate.getDeviceCode(), deviceUpdate);
//            }
//            deviceService.updateBatchById(devicesToUpdate);
//        }
//    }
//}

////        /**
////         * 定时清理过期的设备时间记录
////         */
////        public void cleanExpiredDeviceRecords () {
////            long currentTime = Instant.now().toEpochMilli();
////            deviceStatusMap.entrySet().removeIf(entry -> {
////                long timeDiff = currentTime - entry.getValue();
////                if (timeDiff > RECORD_EXPIRE_TIME) {
////                    log.debug("清理过期设备记录: {}", entry.getKey());
////                    return true;
////                }
////                return false;
////            });
////        }
//
//
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStatusManager {
    // 设备超时时间（毫秒）- 1分钟
    private static final long DEVICE_TIMEOUT = 5 * 60 * 1000;
    // 人体传感的超时时间多一些
    private static final long BODY_SENSOR_TIMEOUT = 10 * 60 * 1000;

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;

    /**
     * 更新设备在线状态
     * 当设备发送数据时调用此方法，自动更新设备状态为在线
     * 并定期检查设备是否超时离线
     */
    public void updateDeviceOnlineStatus(String deviceCode) {
        // 更新设备最后接收数据的时间
        updateDeviceLastDataTime(deviceCode);

        // 检查并更新设备在线状态
        checkAndUpdateAllDeviceStatus();
    }

    /**
     * 更新设备最后接收数据的时间
     */
    private void updateDeviceLastDataTime(String deviceCode) {
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (ObjectUtils.isNotEmpty(device)) {
            device.setLastOnlineTime(Instant.now().toEpochMilli());
            // 将设备最后在线时间存储到Redis中
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, device);
        }

    }

    /**
     * 检查并更新所有设备在线状态
     */
    public void checkAndUpdateAllDeviceStatus() {
        List<Device> devicesToUpdate = new ArrayList<>();

        // 从Redis中获取所有设备的Hash数据
        Map<Object, Object> deviceMap = redisTemplate.opsForHash().entries(RedisConstants.Device.DEVICE);

        for (Map.Entry<Object, Object> entry : deviceMap.entrySet()) {
            String deviceCode = (String) entry.getKey();
            Device device = (Device) entry.getValue();

            if (ObjectUtils.isEmpty(device.getStatus())) {
                continue;
            }

            // 从Redis中获取设备最后在线时间
            Long lastDataTime = device.getLastOnlineTime();

            if (lastDataTime == null) {
                continue;
            }

            long currentTime = Instant.now().toEpochMilli();
            long timeDiff = currentTime - lastDataTime;

            device.setLastOnlineTime(lastDataTime);

            long timeout = DEVICE_TIMEOUT;
            if (device.getDeviceTypeId() == 6) {
                timeout = BODY_SENSOR_TIMEOUT;
            }

            if (timeDiff > timeout) {
                if (device.getStatus() != 0) {
                    device.setStatus(0);
                    devicesToUpdate.add(device);
                    if (device.getDeviceTypeId() == 6) {
                        log.info("人体设备 {} 超时未接收数据，状态已设置为离线", deviceCode);
                    } else {
                        log.info("设备 {} 超时未接收数据，状态已设置为离线", deviceCode);
                    }
                }
            } else {
                if (device.getStatus() != 1) {
                    device.setStatus(1);
                    devicesToUpdate.add(device);
                    if (device.getDeviceTypeId() == 6) {
                        log.info("人体设备 {} 接收到数据，状态已设置为在线", deviceCode);
                    } else {
                        log.info("设备 {} 接收到数据，状态已设置为在线", deviceCode);
                    }
                }
            }
        }

        // 批量更新数据库中的设备状态
        if (!devicesToUpdate.isEmpty()) {
            for (Device deviceUpdate : devicesToUpdate) {
                log.info("更新设备状态: {}", deviceUpdate.toString());
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceUpdate.getDeviceCode(), deviceUpdate);
            }
            deviceService.updateBatchById(devicesToUpdate);
        }
    }
}
