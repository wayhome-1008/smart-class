package com.youlai.boot.device.controller;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.model.dto.Control;
import com.youlai.boot.device.model.dto.ControlParams;
import com.youlai.boot.device.model.dto.Switch;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.floor.model.entity.Floor;
import com.youlai.boot.floor.service.FloorService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final RoomService roomService;
    private final FloorService floorService;

    @Operation(summary = "根据楼、层、房间批量操作设备")
    @PutMapping(value = "/batch")
    @Log(value = "批量操作设备", module = LogModuleEnum.OPERATION)
    public Result<Void> operateDevice(@RequestBody @Validated com.youlai.boot.device.model.form.Operation operation) {
        return switch (operation.getType()) {
            case "room" -> roomOperate(operation);
            case "floor" -> floorOperate(operation);
            default -> Result.failed("暂不支持该操作");
        };
    }

    private Result<Void> floorOperate(com.youlai.boot.device.model.form.Operation operation) {
        //查楼层
        Floor floor = floorService.getById(operation.getId());
        if (ObjectUtils.isEmpty(floor)) return Result.failed("楼层不存在");
        //根据楼层id查询设备
        List<DeviceInfoVO> deviceInfoVOS = deviceService.listDeviceByFloorId(operation.getId(), floor);
        if (ObjectUtils.isEmpty(deviceInfoVOS)) return Result.failed("该楼层没有设备");
        for (DeviceInfoVO deviceInfoVO : deviceInfoVOS) {
            Optional<Integer> tempValue = DeviceInfo.getValueByName(deviceInfoVO.getDeviceInfo(), "count", Integer.class);
            tempValue.ifPresent(value -> this.operate(operation.getOperate(), "-1", value, deviceInfoVO.getDeviceCode(), deviceInfoVO.getDeviceGatewayId(), deviceInfoVO.getCommunicationModeItemId(), deviceInfoVO.getDeviceTypeId()));
        }
        return Result.success();
    }

    private Result<Void> roomOperate(com.youlai.boot.device.model.form.Operation operation) {
        //查本房间的设备
        //查询房间是否存在
        Room room = roomService.getById(operation.getId());
        if (ObjectUtils.isEmpty(room)) return Result.failed("房间不存在");
        //根据房间id查询设备
        List<DeviceInfoVO> deviceInfoVOS = deviceService.listDeviceByRoomId(operation.getId(), room);
        if (ObjectUtils.isEmpty(deviceInfoVOS)) return Result.failed("该房间没有设备");
        for (DeviceInfoVO deviceInfoVO : deviceInfoVOS) {
            Optional<Integer> tempValue = DeviceInfo.getValueByName(deviceInfoVO.getDeviceInfo(), "count", Integer.class);
            tempValue.ifPresent(value -> this.operate(operation.getOperate(), "-1", value, deviceInfoVO.getDeviceCode(), deviceInfoVO.getDeviceGatewayId(), deviceInfoVO.getCommunicationModeItemId(), deviceInfoVO.getDeviceTypeId()));
        }
        return Result.success();
    }

    @Operation(summary = "单设备操作")
    @PutMapping(value = "/{id}")
    @Log(value = "设备操作", module = LogModuleEnum.OPERATION)
    public Result<Void> operateSocket(@Parameter(description = "设备ID") @PathVariable Long id, @RequestBody @Validated DeviceOperate deviceOperate) {
        //根据设备发送mqtt
        Device device = deviceService.getById(id);
        if (ObjectUtils.isEmpty(device)) return Result.failed("设备不存在");
        //状态
        if (device.getStatus() != 1) return Result.failed("该设备非正常状态，无法操作");
        return operate(deviceOperate.getOperate(), deviceOperate.getWay(), deviceOperate.getCount(), device.getDeviceCode(), device.getDeviceGatewayId(), device.getCommunicationModeItemId(), device.getDeviceTypeId());
    }

    private Result<Void> operate(String operate, String way, Integer lightCount, String deviceCode, Long deviceGatewayId, Long deviceCommunicationModeItemId, Long deviceTypeId) {
        if (deviceTypeId != 4 && deviceTypeId != 7 && deviceTypeId != 10 && deviceTypeId != 8)
            return Result.failed("该设备暂无操作");
        //根据通信协议去发送不同协议报文
        return switch (CommunicationModeEnum.getNameById(deviceCommunicationModeItemId)) {
            case "ZigBee" ->
                //zigBee
                    zigBeeDevice(deviceCode, deviceGatewayId, operate, way, lightCount);
            case "MQTT" ->
                //mqtt
                    mqttDevice(deviceCode, operate, way, lightCount);
            default -> Result.failed("暂不支持该协议");
        };
    }

    private Result<Void> mqttDevice(String deviceCode, String operate, String way, Integer lightCount) {
        //目前能控制的就只有灯的开关
        //判断几路
        if (lightCount == 1) {
            try {
                mqttProducer.send("cmnd/" + deviceCode + "/POWER", 0, false, operate);
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }

        } else if (way.equals("-1")) {
            try {
                for (int i = 1; i <= lightCount; i++) {
                    mqttProducer.send("cmnd/" + deviceCode + "/POWER" + i, 0, false, operate);
                }
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        } else {
            try {
                mqttProducer.send("cmnd/" + deviceCode + "/POWER" + way, 0, false, operate);
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        }
        return Result.success();
    }

    private Result<Void> zigBeeDevice(String deviceCode, Long deviceGatewayId, String operate, String way, Integer lightCount) {
        //目前能控制的就只有计量插座和开关
        //查询该子设备的网关
        Device gateway = deviceService.getById(deviceGatewayId);
        if (ObjectUtils.isEmpty(gateway)) return Result.failed("该设备没有网关");
        //组发送json
        if (way.equals("-1")) {
            for (int i = 0; i < lightCount; i++) {
                Control control = new Control();
                control.setDeviceId(deviceCode);
                control.setSequence((int) System.currentTimeMillis());
                Switch plug = new Switch();
                plug.setSwitchStatus(operate.equals("ON") ? "on" : "off");
                plug.setOutlet(i);
                List<Switch> switches = new ArrayList<>();
                switches.add(plug);
                ControlParams controlParams = new ControlParams();
                controlParams.setSwitches(switches);
                control.setParams(controlParams);
                String deviceMac = gateway.getDeviceMac();
                String gateWayTopic = MacUtils.reParseMACAddress(deviceMac);
                try {
                    mqttProducer.send("/zbgw/" + gateWayTopic + "/sub/control", 0, false, JSON.toJSONString(control));
                } catch (MqttException e) {
                    log.error(e.getMessage());
                }
            }
        } else {
            Control control = new Control();
            control.setDeviceId(deviceCode);
            control.setSequence((int) System.currentTimeMillis());
            Switch plug = new Switch();
            plug.setSwitchStatus(operate.equals("ON") ? "on" : "off");
            plug.setOutlet(Integer.parseInt(way) - 1);
            List<Switch> switches = new ArrayList<>();
            switches.add(plug);
            ControlParams controlParams = new ControlParams();
            controlParams.setSwitches(switches);
            control.setParams(controlParams);
            String deviceMac = gateway.getDeviceMac();
            String gateWayTopic = MacUtils.reParseMACAddress(deviceMac);
            log.info(control.toString());
            try {
                mqttProducer.send("/zbgw/" + gateWayTopic + "/sub/control", 0, false, JSON.toJSONString(control));
            } catch (MqttException e) {
                log.error(e.getMessage());
            }
        }
        return Result.success();

    }
}
