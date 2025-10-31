package com.youlai.boot.device.operation;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.model.dto.Control;
import com.youlai.boot.device.model.dto.Switch;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.service.DeviceService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.youlai.boot.device.operation.OperationUtils.*;

/**
 *@Author: way
 *@CreateTime: 2025-09-11  15:54
 *@Description: TODO
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceOperation {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;

    //根据场景操作设备
    public void operateForScene(DeviceOperate deviceOperate, String deviceCode, MqttClient mqttClient) throws MqttException {
        if (deviceOperate == null || StringUtils.isEmpty(deviceCode)) {
            log.warn("[场景执行]operateForScene 参数非法: deviceOperate={}, deviceCode={}", deviceOperate, deviceCode);
            return;
        }

        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (ObjectUtils.isEmpty(device)) {
            log.warn("[场景执行]: deviceCode={}", deviceCode);
            return;
        }
        if (device.getIsLock() == 1) return;
        JsonNode deviceInfo = device.getDeviceInfo();
        if (deviceInfo.isEmpty()) {
            log.warn("[场景执行]设备信息为空: deviceCode={}", deviceCode);
            return;
        }

        String way = deviceOperate.getWay();
        if (StringUtils.isEmpty(way)) {
            log.warn("[场景执行]操作路数为空: deviceCode={}", deviceCode);
            return;
        }

        JsonNode wayNode = deviceInfo.get("switch" + way);
        if (wayNode.isEmpty()) {
            log.warn("[场景执行]设备路数信息不存在: deviceCode={}, way={}", deviceCode, way);
            return;
        }

        String wayStatus = wayNode.asText();
        String operate = deviceOperate.getOperate();
        if (StringUtils.isEmpty(operate)) {
            log.warn("[场景执行]操作指令为空: deviceCode={}", deviceCode);
            return;
        }

        if (Objects.equals(wayStatus, operate)) {
            log.info("[场景执行]设备状态一致，无需操作: deviceCode={}, way={}, status={}", deviceCode, way, operate);
            return;
        }

        if (isUnSupportedDeviceType(device.getDeviceTypeId())) {
            log.warn("[场景执行]不支持的设备类型: deviceTypeId={}", device.getDeviceTypeId());
            return;
        }

        if (device.getIsLock() == 1) return;

        //根据通信协议去发送不同协议报文
        String protocol = CommunicationModeEnum.getNameById(device.getCommunicationModeItemId());
        switch (protocol) {
            case "ZigBee" ->
                    zigBeeDevice(device.getDeviceCode(), device.getDeviceGatewayId(), operate, way, mqttClient);
            case "WiFi" -> wifiDevice(device.getDeviceCode(), operate, way, mqttClient);
            default -> log.warn("[场景执行]暂不支持该协议: protocol={}", protocol);
        }
    }

    // 根据定时任务操作设备
    public void operateForSchedule(DeviceOperate deviceOperate, Long deviceId, MqttClient mqttClient) throws MqttException {
        if (deviceOperate == null || deviceId == null) {
            log.warn("[任务执行]operateForSchedule 参数非法: deviceOperate={}, deviceId={}", deviceOperate, deviceId);
            return;
        }

        // 根据设备ID获取设备信息
        Device device = deviceService.getById(deviceId);
        if (ObjectUtils.isEmpty(device)) {
            log.warn("[任务执行]设备未找到: deviceId={}", deviceId);
            return;
        }
        if (device.getIsLock() == 1) return;

        String deviceCode = device.getDeviceCode();
        Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        assert deviceCache != null;
        JsonNode deviceInfo = deviceCache.getDeviceInfo();
        if (deviceInfo.isEmpty()) {
            log.warn("[任务执行]设备信息为空: deviceCode={}", deviceCode);
            return;
        }

        String way = deviceOperate.getWay();
        if (StringUtils.isEmpty(way)) {
            log.warn("[任务执行]操作路数为空: deviceCode={}", deviceCode);
            return;
        }

        JsonNode wayNode = deviceInfo.get("switch" + way);
        if (wayNode.isEmpty()) {
            log.warn("[任务执行]设备路数信息不存在: deviceCode={}, way={}", deviceCode, way);
            return;
        }

        String wayStatus = wayNode.asText();
        String operate = deviceOperate.getOperate();
        if (StringUtils.isEmpty(operate)) {
            log.warn("[任务执行]操作指令为空: deviceCode={}", deviceCode);
            return;
        }

        if (Objects.equals(wayStatus, operate)) {
            log.info("[任务执行]设备状态一致，无需操作: deviceCode={}, way={}, status={}", deviceCode, way, operate);
            return;
        }

        if (isUnSupportedDeviceType(device.getDeviceTypeId())) {
            log.warn("[任务执行]不支持的设备类型: deviceTypeId={}", device.getDeviceTypeId());
            return;
        }

        if (device.getIsLock() == 1) return;

        // 根据通信协议去发送不同协议报文
        String protocol = CommunicationModeEnum.getNameById(device.getCommunicationModeItemId());
        switch (protocol) {
            case "ZigBee" ->
                    zigBeeDevice(device.getDeviceCode(), device.getDeviceGatewayId(), operate, way, mqttClient);
            case "WiFi" -> wifiDevice(device.getDeviceCode(), operate, way, mqttClient);
            default -> log.warn("[任务执行]暂不支持该协议: protocol={}", protocol);
        }
    }

    //接口操作
    public Result<Void> operate(Long deviceId, DeviceOperate operation, MqttProducer mqttProducer) {
        if (operation == null || deviceId == null) {
            log.warn("[接口执行]operateForSchedule 参数非法: operation={}, deviceId={}", operation, deviceId);
            return Result.failed("参数非法");
        }

        // 根据设备ID获取设备信息
        Device device = deviceService.getById(deviceId);
        if (ObjectUtils.isEmpty(device)) {
            log.warn("[接口执行]设备未找到: deviceId={}", deviceId);
            return Result.failed("设备未找到");
        }

        String deviceCode = device.getDeviceCode();
        Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        JsonNode deviceInfo = deviceCache.getDeviceInfo();
        if (deviceInfo.isEmpty()) {
            log.warn("[接口执行]设备信息为空: deviceCode={}", deviceCode);
            return Result.failed("设备信息为空");
        }
        String way = operation.getWay();
        if (StringUtils.isEmpty(way)) {
            log.warn("[接口执行]操作路数为空: deviceCode={}", deviceCode);
            return Result.failed("操作路数为空");
        }

        if (!way.equals("-1")) {
            JsonNode wayNode = deviceInfo.get("switch" + way);
            if (wayNode.isEmpty()) {
                log.warn("[接口执行]设备路数信息不存在: deviceCode={}, way={}", deviceCode, way);
                return Result.failed("设备路数信息不存在");
            }

            String wayStatus = wayNode.asText();
            String operate = operation.getOperate();
            if (StringUtils.isEmpty(operate)) {
                log.warn("[接口执行]操作指令为空: deviceCode={}", deviceCode);
                return Result.failed("操作指令为空");
            }

            if (Objects.equals(wayStatus, operate)) {
                log.info("[接口执行]设备状态一致，无需操作: deviceCode={}, way={}, status={}", deviceCode, way, operate);
                return Result.failed("设备状态一致，无需操作");
            }
        }

        if (isUnSupportedDeviceType(device.getDeviceTypeId())) {
            log.warn("[接口执行]不支持的设备类型: deviceTypeId={}", device.getDeviceTypeId());
            return Result.failed("不支持的设备类型");
        }

        if (device.getIsLock() == 1) return Result.failed("设备被锁定");

        //对count为=0 表示批量
        if (operation.getCount() == 0) {
            int count = deviceInfo.get("count").asInt();
            operation.setCount(count);
        }
        return switch (CommunicationModeEnum.getNameById(device.getCommunicationModeItemId())) {
            case "ZigBee" -> zigBeeDeviceOperate(deviceCode, device.getDeviceGatewayId(), operation, mqttProducer);
            case "WiFi" -> wifiDeviceOperate(deviceCode, operation, mqttProducer);
            default -> Result.failed("暂不支持该协议");
        };
    }

    private Result<Void> wifiDeviceOperate(String deviceCode, DeviceOperate operation, MqttProducer mqttProducer) {
        //目前能控制的就只有灯的开关
        //判断几路
        if (operation.getCount() == 1) {
            try {
                mqttProducer.send(WIFI_TOPIC_PREFIX + deviceCode + WIFI_TOPIC_SUFFIX, 0, false, operation.getOperate());
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        } else if (operation.getWay().equals("-1")) {
            try {
                for (int i = 1; i <= operation.getCount(); i++) {
                    mqttProducer.send(WIFI_TOPIC_PREFIX + deviceCode + WIFI_TOPIC_SUFFIX + i, 0, false, operation.getOperate());
                }
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        } else {
            try {
                mqttProducer.send(OperationUtils.makeWifiTopic(deviceCode, operation.getWay()), 0, false, operation.getOperate());
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        }
        return Result.success();
    }

    private Result<Void> zigBeeDeviceOperate(String deviceCode, Long deviceGatewayId, DeviceOperate operation, MqttProducer mqttProducer) {
        //目前能控制的就只有计量插座和开关
        //查询该子设备的网关
        Device gateway = deviceService.getById(deviceGatewayId);
        if (ObjectUtils.isEmpty(gateway)) return Result.failed("该设备没有网关");
        //组发送json
        if (operation.getWay().equals("-1")) {
            for (int i = 0; i < operation.getCount(); i++) {
                Control control = makeControl(deviceCode);
                Switch plug = makeSwitch(operation.getOperate(), i);
                List<Switch> switches = new ArrayList<>();
                switches.add(plug);
                makeControlParams(switches, control);
                String deviceMac = gateway.getDeviceMac();
                String gateWayTopic = MacUtils.reParseMACAddress(deviceMac);
                try {
                    mqttProducer.send(OperationUtils.makeZigBeeTopic(gateWayTopic), 0, false, JSON.toJSONString(control));
                } catch (MqttException e) {
                    log.error(e.getMessage());
                }
            }
        } else {
            Control control = makeControl(deviceCode);
            Switch plug = makeSwitch(operation.getOperate(), Integer.parseInt(operation.getWay()) - 1);
            List<Switch> switches = new ArrayList<>();
            switches.add(plug);
            makeControlParams(switches, control);
            String deviceMac = gateway.getDeviceMac();
            String gateWayTopic = MacUtils.reParseMACAddress(deviceMac);
            log.info(control.toString());
            try {
                mqttProducer.send(OperationUtils.makeZigBeeTopic(gateWayTopic), 0, false, JSON.toJSONString(control));
            } catch (MqttException e) {
                log.error(e.getMessage());
            }
        }
        return Result.success();

    }

    private boolean isUnSupportedDeviceType(Long deviceTypeId) {
        return deviceTypeId == null || (deviceTypeId != 4 && deviceTypeId != 7 && deviceTypeId != 10 && deviceTypeId != 8);
    }

    /**
     * @description: 定时任务以及场景适用的通用方法
     * @author: way
     * @date: 2025/9/12 16:30
     * @param: [deviceCode, deviceGatewayId, operate, way, mqttClient]
     * @return: void
     **/
    private void zigBeeDevice(String deviceCode, Long deviceGatewayId, @Pattern(regexp = "ON|OFF", message = "错误操作") String operate, String way, MqttClient mqttClient) throws MqttException {
        try {
            if (StringUtils.isEmpty(way) || !way.matches("\\d+")) {
                log.warn("非法路数参数: way={}", way);
                return;
            }

            Device gateway = deviceService.getById(deviceGatewayId);
            if (ObjectUtils.isEmpty(gateway)) {
                log.warn("网关设备未找到: gatewayId={}", deviceGatewayId);
                return;
            }

            String gateWayTopic = MacUtils.reParseMACAddress(gateway.getDeviceMac());
            if (StringUtils.isEmpty(gateWayTopic)) {
                log.warn("网关Topic为空: gatewayId={}", deviceGatewayId);
                return;
            }

            Control control = makeControl(deviceCode);
            Switch plug = makeSwitch(operate, Integer.parseInt(way) - 1);
            List<Switch> switches = new ArrayList<>();
            switches.add(plug);
            makeControlParams(switches, control);
            mqttClient.publish(makeZigBeeTopic(gateWayTopic), JSON.toJSONString(control).getBytes(), 2, false);
        } catch (MqttException e) {
            log.error("ZigBee设备发送消息失败: deviceCode={}, gatewayId={}", deviceCode, deviceGatewayId, e);
            throw e; // 重新抛出异常，让调用方感知
        } catch (NumberFormatException e) {
            log.error("路数转换失败: way={}", way, e);
        }
    }

    /**
     * @description: 定时任务以及场景适用的通用方法
     * @author: way
     * @date: 2025/9/12 16:30
     * @param: [deviceCode, operate, way, mqttClient]
     * @return: void
     **/
    private void wifiDevice(String deviceCode, String operate, String way, MqttClient mqttClient) throws MqttException {
        try {
            if (StringUtils.isEmpty(deviceCode) || StringUtils.isEmpty(operate) || StringUtils.isEmpty(way)) {
                log.warn("WiFi设备操作参数缺失: deviceCode={}, operate={}, way={}", deviceCode, operate, way);
                return;
            }
            mqttClient.publish(makeWifiTopic(deviceCode, way), operate.getBytes(), 1, false);
        } catch (MqttException e) {
            log.error("WiFi设备发送消息失败: deviceCode={}, way={}", deviceCode, way, e);
            throw e; // 重新抛出异常
        }
    }
}
