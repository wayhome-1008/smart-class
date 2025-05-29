package com.youlai.boot.device.controller;

import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  17:10
 *@Description: TODO
 */
@Slf4j
@Tag(name = "05.设备操作接口")
@RestController
@RequestMapping("/api/v1/device/Operate")
@RequiredArgsConstructor
@Transactional
public class DeviceOperateController {
    private final DeviceService deviceService;
    private final MqttProducer mqttProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "灯光操作")
    @PutMapping(value = "/{id}")
    public Result<Void> operateDevice(
            @Parameter(description = "设备ID") @PathVariable Long id,
            @RequestBody @Validated DeviceOperate
                    deviceOperate
    ) throws MqttException {
        //根据设备发送mqtt
        Device device = deviceService.getById(id);
        String deviceCode = device.getDeviceCode();
        //判断几路
        int lightCount = device.getDeviceInfo().get("count").asInt();
        if (lightCount == 1) {
            mqttProducer.send("cmnd/" + deviceCode + "/POWER", 0, false, deviceOperate.getOperate());
        } else {
            mqttProducer.send("cmnd/" + deviceCode + "/POWER" + deviceOperate.getWay(), 0, false, deviceOperate.getOperate());
        }
        return Result.success();
    }
}
