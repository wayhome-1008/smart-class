package com.youlai.boot.device.controller;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.Enum.DeviceTypeEnum;
import com.youlai.boot.device.factory.DeviceInfoParserFactory;
import com.youlai.boot.device.model.dto.Control;
import com.youlai.boot.device.model.dto.ControlParams;
import com.youlai.boot.device.model.dto.Switch;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.service.DeviceInfoParser;
import com.youlai.boot.device.service.DeviceService;
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

import static com.youlai.boot.dashBoard.controller.DashBoardController.basicPropertyConvert;

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

    //    @Operation(summary = "灯光操作")
//    @PutMapping(value = "/{id}")
//    public Result<Void> operateDevice(
//            @Parameter(description = "设备ID") @PathVariable Long id,
//            @RequestBody @Validated DeviceOperate
//                    deviceOperate
//    ) throws MqttException {
//        //根据设备发送mqtt
//        Device device = deviceService.getById(id);
//        if (ObjectUtils.isEmpty(device)) return Result.failed("设备不存在");
//        //校验是否为灯
//        if (device.getDeviceTypeId() != 8) return Result.failed("该设备不是灯");
//        String deviceCode = device.getDeviceCode();
//        //判断几路
//        int lightCount = device.getDeviceInfo().get("count").asInt();
//        if (lightCount == 1) {
//            mqttProducer.send("cmnd/" + deviceCode + "/POWER", 0, false, deviceOperate.getOperate());
//        } else {
//            mqttProducer.send("cmnd/" + deviceCode + "/POWER" + deviceOperate.getWay(), 0, false, deviceOperate.getOperate());
//        }
//        return Result.success();
//    }
    @Operation(summary = "根据楼、层、房间批量操作设备")
    @PutMapping(value = "/batch")
    public Result<List<DeviceInfoVO>> operateDevice(
            @RequestBody @Validated com.youlai.boot.device.model.form.Operation operation) {
        return switch (operation.getType()) {
            case "room" ->
                //房间
                    roomOperate(operation);
//            case "floor" ->
//                //楼层
//                    floorOperate(operation);
//            case "building" ->
//                //楼
//                    buildingOperate(operation);
            default -> Result.failed("暂不支持该操作");
        };
    }

    private Result<List<DeviceInfoVO>> roomOperate(com.youlai.boot.device.model.form.Operation operation) {
        //查本房间的设备
        //查询房间是否存在
        Room room = roomService.getById(operation.getId());
        if (ObjectUtils.isEmpty(room)) return Result.failed("房间不存在");
        //根据房间id查询设备
        List<DeviceInfoVO> deviceInfoVOS = deviceService.listDeviceByRoomId(operation.getId(), room);
        for (DeviceInfoVO deviceInfoVO : deviceInfoVOS) {
           this.operate()
        }
    }

    @Operation(summary = "插座操作")
    @PutMapping(value = "/socket/{id}")
    public Result<Void> operateSocket(
            @Parameter(description = "设备ID") @PathVariable Long id,
            @RequestBody @Validated DeviceOperate
                    deviceOperate
    ) throws MqttException {
        //根据设备发送mqtt
        Device device = deviceService.getById(id);
        if (ObjectUtils.isEmpty(device)) return Result.failed("设备不存在");
        return operate(deviceOperate, device);
    }

    private Result<Void> operate(DeviceOperate deviceOperate, Device device) throws MqttException {
        if (device.getDeviceTypeId() != 3 && device.getDeviceTypeId() != 4 && device.getDeviceTypeId() != 7 && device.getDeviceTypeId() != 10 && device.getDeviceTypeId() != 8)
            return Result.failed("该设备暂无操作");
        //根据通信协议去发送不同协议报文
        return switch (CommunicationModeEnum.getNameById(device.getCommunicationModeItemId())) {
            case "ZigBee" ->
                //zigBee
                    zigBeeDevice(device, deviceOperate);
            case "MQTT" ->
                //mqtt
                    mqttDevice(device, deviceOperate);
            default -> Result.failed("暂不支持该协议");
        };
    }

    private Result<Void> mqttDevice(Device device, DeviceOperate
            deviceOperate) throws MqttException {
        //目前能控制的就只有灯的开关
        //判断几路
        int lightCount = deviceOperate.getCount();
        if (lightCount == 1) {
            mqttProducer.send("cmnd/" + device.getDeviceCode() + "/POWER", 0, false, deviceOperate.getOperate());
        } else if (deviceOperate.getWay().equals("-1")) {
            for (int i = 1; i <= lightCount; i++) {
                mqttProducer.send("cmnd/" + device.getDeviceCode() + "/POWER" + i, 0, false, deviceOperate.getOperate());
            }
        } else {
            mqttProducer.send("cmnd/" + device.getDeviceCode() + "/POWER" + deviceOperate.getWay(), 0, false, deviceOperate.getOperate());
        }
        return Result.success();
    }

    private Result<Void> zigBeeDevice(Device device, DeviceOperate
            deviceOperate) throws MqttException {
        //目前能控制的就只有计量插座和开关
        //查询该子设备的网关
        Device gateway = deviceService.getById(device.getDeviceGatewayId());
        if (ObjectUtils.isEmpty(gateway)) return Result.failed("该设备没有网关");
        //组发送json
        if (deviceOperate.getWay().equals("-1")) {
            for (int i = 0; i < deviceOperate.getCount(); i++) {
                Control control = new Control();
                control.setDeviceId(device.getDeviceCode());
                control.setSequence((int) System.currentTimeMillis());
                Switch plug = new Switch();
                plug.setSwitchStatus(deviceOperate.getOperate().equals("ON") ? "on" : "off");
                plug.setOutlet(i);
                List<Switch> switches = new java.util.ArrayList<>();
                switches.add(plug);
                ControlParams controlParams = new ControlParams();
                controlParams.setSwitches(switches);
                control.setParams(controlParams);
                String deviceMac = gateway.getDeviceMac();
                String gateWayTopic = MacUtils.reParseMACAddress(deviceMac);
                log.info(control.toString());
                mqttProducer.send("/zbgw/" + gateWayTopic + "/sub/control", 0, false, JSON.toJSONString(control));
            }
            return Result.success();
        } else {
            Control control = new Control();
            control.setDeviceId(device.getDeviceCode());
            control.setSequence((int) System.currentTimeMillis());
            Switch plug = new Switch();
            plug.setSwitchStatus(deviceOperate.getOperate().equals("ON") ? "on" : "off");
            plug.setOutlet(Integer.parseInt(deviceOperate.getWay()) - 1);
            List<Switch> switches = new java.util.ArrayList<>();
            switches.add(plug);
            ControlParams controlParams = new ControlParams();
            controlParams.setSwitches(switches);
            control.setParams(controlParams);
            String deviceMac = gateway.getDeviceMac();
            String gateWayTopic = MacUtils.reParseMACAddress(deviceMac);
            log.info(control.toString());
            mqttProducer.send("/zbgw/" + gateWayTopic + "/sub/control", 0, false, JSON.toJSONString(control));
            return Result.success();
        }

    }
}
