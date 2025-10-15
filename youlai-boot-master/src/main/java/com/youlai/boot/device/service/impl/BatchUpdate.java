package com.youlai.boot.device.service.impl;

import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *@Author: way
 *@CreateTime: 2025-06-23  12:07
 *@Description: TODO
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchUpdate {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;

    // 定时任务：从Redis读取设备状态并更新到数据库
    @Scheduled(fixedDelay = 120000, initialDelay = 120000)  // 延迟2分钟执行首次
    @Transactional
    public void batchUpdateToDatabase() {
        // 获取所有的设备
        Map<Object, Object> device = redisTemplate.opsForHash().entries(RedisConstants.Device.DEVICE);
        List<Device> threeWay = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : device.entrySet()) {
            // 假设value就是Device对象，直接强制类型转换
            Device deviceObj = (Device) entry.getValue();
            threeWay.add(deviceObj);
        }
        deviceService.updateBatchById(threeWay);
    }
}
