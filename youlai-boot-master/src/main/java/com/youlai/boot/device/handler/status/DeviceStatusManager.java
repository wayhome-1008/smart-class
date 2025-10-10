package com.youlai.boot.device.handler.status;

import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *@Author: way
 *@CreateTime: 2025-09-01  15:11
 *@Description: 负责跟踪设备的在线状态，自动检测设备离线情况
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStatusManager {
    // 使用ConcurrentHashMap存储设备最后接收数据的时间
    public static final ConcurrentHashMap<String, Long> deviceLastDataTimeMapWIFI = new ConcurrentHashMap<>();

    // 设备超时时间（毫秒）- 1分钟
    private static final long DEVICE_TIMEOUT = 10 * 60 * 1000;

    // 设备记录过期时间（毫秒）- 5分钟
    private static final long RECORD_EXPIRE_TIME = 5 * 60 * 1000;

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;

    /**
     * 更新设备在线状态
     * 当设备发送数据时调用此方法，自动更新设备状态为在线
     * 并定期检查设备是否超时离线
     *
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
        deviceLastDataTimeMapWIFI.put(deviceCode, Instant.now().toEpochMilli());
    }

    /**
     * 检查并更新所有设备在线状态
     */
    public void checkAndUpdateAllDeviceStatus() {
        List<Device> devicesToUpdate = new ArrayList<>();

        for (Map.Entry<String, Long> entry : deviceLastDataTimeMapWIFI.entrySet()) {
            String deviceCode = entry.getKey();
            Long lastDataTime = entry.getValue();

            long currentTime = Instant.now().toEpochMilli();
            long timeDiff = currentTime - lastDataTime;

            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (device == null) continue;
            if (Objects.equals(device.getCommunicationModeItemName(), "WIFI")) {
                if (timeDiff > DEVICE_TIMEOUT) {
                    if (device.getStatus() != 0) {
                        device.setStatus(0);
                        devicesToUpdate.add(device);
                        log.info("设备 {} 超时未接收数据，状态已设置为离线", deviceCode);
                    }
                } else {
                    if (device.getStatus() != 1) {
                        device.setStatus(1);
                        devicesToUpdate.add(device);
                        log.info("设备 {} 接收到数据，状态已设置为在线", deviceCode);
                    }
                }
            }
        }

        // 批量更新数据库中的设备状态
        if (!devicesToUpdate.isEmpty()) {
            for (Device device : devicesToUpdate) {
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
            }
            deviceService.updateBatchById(devicesToUpdate);
        }
    }


    /**
     * 定时清理过期的设备时间记录
     */
    public void cleanExpiredDeviceRecords() {
        long currentTime = Instant.now().toEpochMilli();
        deviceLastDataTimeMapWIFI.entrySet().removeIf(entry -> {
            long timeDiff = currentTime - entry.getValue();
            if (timeDiff > RECORD_EXPIRE_TIME) {
                log.debug("清理过期设备记录: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
