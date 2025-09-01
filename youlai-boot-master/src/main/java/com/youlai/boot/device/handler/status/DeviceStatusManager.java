package com.youlai.boot.device.handler.status;

import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 *@Author: way
 *@CreateTime: 2025-09-01  15:11
 *@Description: 负责跟踪设备的在线状态，自动检测设备离线情况
 */
@Slf4j
@Component
public class DeviceStatusManager {
    // 使用ConcurrentHashMap存储设备最后接收数据的时间
    private static final ConcurrentHashMap<String, Long> deviceLastDataTimeMap = new ConcurrentHashMap<>();

    // 设备超时时间（毫秒）- 1分钟
    private static final long DEVICE_TIMEOUT = 60 * 1000;

    // 设备记录过期时间（毫秒）- 5分钟
    private static final long RECORD_EXPIRE_TIME = 5 * 60 * 1000;

    /**
     * 更新设备在线状态
     * 当设备发送数据时调用此方法，自动更新设备状态为在线
     * 并定期检查设备是否超时离线
     *
     * @param deviceCode 设备编码
     * @param device 设备对象
     * @param deviceService 设备服务
     */
    public void updateDeviceOnlineStatus(String deviceCode, Device device, DeviceService deviceService) {
        // 更新设备最后接收数据的时间
        updateDeviceLastDataTime(deviceCode);

        // 检查并更新设备在线状态
        checkAndUpdateDeviceStatus(device, deviceService);
    }

    /**
     * 更新设备最后接收数据的时间
     * @param deviceCode 设备编码
     */
    private void updateDeviceLastDataTime(String deviceCode) {
        deviceLastDataTimeMap.put(deviceCode, Instant.now().toEpochMilli());
    }

    /**
     * 检查并更新设备在线状态
     * @param device 设备对象
     * @param deviceService 设备服务
     */
    private void checkAndUpdateDeviceStatus(Device device, DeviceService deviceService) {
        Long lastDataTime = deviceLastDataTimeMap.get(device.getDeviceCode());

        if (lastDataTime != null) {
            long currentTime = Instant.now().toEpochMilli();
            long timeDiff = currentTime - lastDataTime;

            // 如果超过1分钟没有收到数据，将设备状态设置为离线
            if (timeDiff > DEVICE_TIMEOUT) { // 60秒 = 1分钟
                if (device.getStatus() != 0) { // 0表示离线，1表示在线
                    device.setStatus(0);
                    // 更新数据库中的设备状态
                    deviceService.updateById(device);
                    log.info("设备 {} 超时未接收数据，状态已设置为离线", device.getDeviceCode());
                }
            } else {
                // 如果在1分钟内收到了数据，确保设备状态为在线
                if (device.getStatus() != 1) {
                    device.setStatus(1);
                    // 更新数据库中的设备状态
                    deviceService.updateById(device);
                    log.info("设备 {} 接收到数据，状态已设置为在线", device.getDeviceCode());
                }
            }
        } else {
            // 如果没有记录最后数据时间，初始化为当前时间
            updateDeviceLastDataTime(device.getDeviceCode());
            if (device.getStatus() != 1) {
                device.setStatus(1);
                deviceService.updateById(device);
                log.info("设备 {} 初始化在线状态", device.getDeviceCode());
            }
        }
    }

    /**
     * 定时清理过期的设备时间记录
     */
    public void cleanExpiredDeviceRecords() {
        long currentTime = Instant.now().toEpochMilli();

        deviceLastDataTimeMap.entrySet().removeIf(entry -> {
            long timeDiff = currentTime - entry.getValue();
            if (timeDiff > RECORD_EXPIRE_TIME) {
                log.debug("清理过期设备记录: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 手动设置设备为离线状态
     * @param deviceCode 设备编码
     */
    public void setDeviceOffline(String deviceCode) {
        deviceLastDataTimeMap.remove(deviceCode);
    }

    /**
     * 检查设备是否在线
     * @param deviceCode 设备编码
     * @return true表示在线，false表示离线或未知
     */
    public boolean isDeviceOnline(String deviceCode) {
        Long lastDataTime = deviceLastDataTimeMap.get(deviceCode);
        if (lastDataTime == null) {
            return false;
        }

        long currentTime = Instant.now().toEpochMilli();
        long timeDiff = currentTime - lastDataTime;
        return timeDiff <= DEVICE_TIMEOUT;
    }

    /**
     * 获取设备最后活跃时间
     * @param deviceCode 设备编码
     * @return 最后活跃时间戳，如果设备不存在则返回null
     */
    public Long getDeviceLastActiveTime(String deviceCode) {
        return deviceLastDataTimeMap.get(deviceCode);
    }
}
