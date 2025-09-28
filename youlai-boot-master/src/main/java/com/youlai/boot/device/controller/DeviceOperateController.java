package com.youlai.boot.device.controller;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.model.form.DeviceOperateBatch;
import com.youlai.boot.device.model.form.SerialData;
import com.youlai.boot.device.model.form.SerialDataDown;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.operation.DeviceOperation;
import com.youlai.boot.device.operation.OperationUtils;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
@Transactional(rollbackFor = Exception.class)
public class DeviceOperateController {
    private final DeviceService deviceService;
    private final MqttProducer mqttProducer;
    private final RoomService roomService;
    private final FloorService floorService;
    private final DeviceOperation deviceOperation;

    @Operation(summary = "串口透传demo")
    @GetMapping(value = "/serial{id}")
    public Result<SerialDataDown> serialTransfer(@Parameter(description = "设备ID") @PathVariable Long id, @RequestParam(required = false) String data) {
        //根据设备发送mqtt
        Device device = deviceService.getById(id);
        if (ObjectUtils.isEmpty(device)) return Result.failed("设备不存在");
        //状态
        if (device.getStatus() != 1) return Result.failed("该设备非正常状态，无法操作");
        //查询网关
        Device gateway = deviceService.getById(device.getDeviceGatewayId());
        if (ObjectUtils.isEmpty(gateway)) return Result.failed("该设备没有网关");
        //构造透传 数据
        SerialData serialData = new SerialData();
        serialData.setData(data);
        serialData.setTunnelingId(0);
        SerialDataDown serialDataDown = new SerialDataDown();
        serialDataDown.setDeviceId(device.getDeviceCode());
        serialDataDown.setSerialData(serialData);
        serialDataDown.setSequence(123);
        //发送mqtt
        try {
            String topic = "/zbgw/" + gateway.getDeviceCode() + "/sub/control";
            log.info(topic);
            mqttProducer.send(topic, 0, false, JSON.toJSONString(serialDataDown));
        } catch (MqttException e) {
            log.error("串口透传发送失败", e);
            return Result.failed("串口透传发送失败");
        }
        return Result.success(serialDataDown);
    }

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
        return getVoidResult(operation, deviceInfoVOS);
    }

    private Result<Void> roomOperate(com.youlai.boot.device.model.form.Operation operation) {
        //查本房间的设备
        //查询房间是否存在
        Room room = roomService.getById(operation.getId());
        if (ObjectUtils.isEmpty(room)) return Result.failed("房间不存在");
        //根据房间id查询设备
        List<DeviceInfoVO> deviceInfoVOS = deviceService.listDeviceByRoomId(operation.getId(), room);
        if (ObjectUtils.isEmpty(deviceInfoVOS)) return Result.failed("该房间没有设备");
        return getVoidResult(operation, deviceInfoVOS);
    }

    @NotNull
    private Result<Void> getVoidResult(com.youlai.boot.device.model.form.Operation operation, List<DeviceInfoVO> deviceInfoVOS) {
        for (DeviceInfoVO deviceInfoVO : deviceInfoVOS) {
            Optional<Integer> count = DeviceInfo.getValueByName(deviceInfoVO.getDeviceInfo(), "count", Integer.class);
            count.ifPresent(
                    value -> deviceOperation.operate(deviceInfoVO.getId(), OperationUtils.convert(operation, value, "-1"), mqttProducer));
        }
        return Result.success();
    }

    @Operation(summary = "单设备操作")
    @PutMapping(value = "/{id}")
    @Log(value = "对单设备操作", module = LogModuleEnum.OPERATION)
    public Result<Void> operateSocket(@Parameter(description = "设备ID") @PathVariable Long id, @RequestBody @Validated DeviceOperate deviceOperate) {
        return deviceOperation.operate(id, deviceOperate, mqttProducer);
    }

    @Operation(summary = "多设备操作(用于设备批量操作)")
    @PostMapping(value = "/batch")
    @Log(value = "多设备操作", module = LogModuleEnum.OPERATION)
//    public Result<Void> operateSocket(@RequestBody @Validated List<DeviceOperateBatch> deviceOperate) {
    public Result<Void> operateSocket(@Parameter(description = "设备IDs") String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        for (Long id : idList) {
            DeviceOperate deviceOperate = new DeviceOperate();
            deviceOperate.setOperate("OFF");
            deviceOperate.setWay("-1");
            deviceOperate.setCount(1);
            return deviceOperation.operate(id, deviceOperate, mqttProducer);
        }
//        for (DeviceOperateBatch deviceOperateBatch : deviceOperate) {
//            DeviceOperate convert = convert(deviceOperateBatch);
//            deviceOperation.operate(deviceOperateBatch.getDeviceId(), convert, mqttProducer);
//        }
        return Result.success();
    }


    public DeviceOperate convert(DeviceOperateBatch deviceOperateBatch) {
        DeviceOperate deviceOperate = new DeviceOperate();
        deviceOperate.setOperate(deviceOperateBatch.getOperate());
        deviceOperate.setWay(deviceOperateBatch.getWay());
        deviceOperate.setCount(deviceOperateBatch.getCount());
        return deviceOperate;
    }

}
