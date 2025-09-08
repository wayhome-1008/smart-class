package com.youlai.boot.room.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.MathUtils;
import com.youlai.boot.dashBoard.service.ElectricityCalculationService;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.model.form.RoomForm;
import com.youlai.boot.room.model.query.RoomQuery;
import com.youlai.boot.room.model.vo.RoomVO;
import com.youlai.boot.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 房间管理前端控制层
 *
 * @author way
 * @since 2025-05-09 12:09
 */
@Tag(name = "08.房间管理接口")
@RestController
@RequestMapping("/api/v1/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final DeviceService deviceService;
    private final ElectricityCalculationService electricityCalculationService;

    @Operation(summary = "房间管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('room:room:query')")
    public PageResult<RoomVO> getRoomPage(RoomQuery queryParams) {
        // 1. 获取房间分页数据
        IPage<RoomVO> result = roomService.getRoomPage(queryParams);
        if (result.getTotal() == 0) {
            return PageResult.success(result);
        }
        // 2. 批量获取所有相关设备
        List<DeviceInfoVO> devices = deviceService.listDeviceByRoomIds(result.getRecords());
        if (ObjectUtils.isNotEmpty(devices)) {
            // 3. 按房间ID分组设备
            Map<Long, List<DeviceInfoVO>> deviceMap = devices.stream()
                    .collect(Collectors.groupingBy(DeviceInfoVO::getDeviceRoom));

            // 4. 为每个房间设置设备及状态
            result.getRecords().forEach(roomVO -> {
                List<DeviceInfoVO> roomDevices = deviceMap.getOrDefault(roomVO.getId(), new ArrayList<>());
                roomVO.setDeviceInfo(roomDevices); // 一次性设置完整设备列表
                // 初始化房间的状态指标
                initRoomStatusIndicators(roomVO, roomDevices);
                //根据roomDevices查询influxdb获取当天用电数据
                double todayElectricity = 0.0;
                for (DeviceInfoVO roomDevice : roomDevices) {
                    todayElectricity = todayElectricity + electricityCalculationService.calculateTodayElectricity(roomDevice.getDeviceCode(), String.valueOf(roomVO.getId()));
                }
                roomVO.setTodayElectricity(MathUtils.formatDouble(todayElectricity));
            });
        }

        return PageResult.success(result);
    }

    // 初始化房间的状态指标的方法
    private void initRoomStatusIndicators(RoomVO roomVO, List<DeviceInfoVO> devices) {
        // 默认值设置
        roomVO.setLight(false);
        roomVO.setPlug(false);
        roomVO.setHuman(false);
        roomVO.setLightNum(0);
        roomVO.setPlugNum(0);
        for (DeviceInfoVO device : devices) {
            if (ObjectUtils.isNotEmpty((device.getCategoryId()))) {
                if (device.getStatus() == 1) {
                    switch (device.getCategoryId().intValue()) {
                        case 6: // 2->温湿度传感器
                            DeviceInfo.getValueByName(device.getDeviceInfo(), "temperature", Double.class)
                                    .map(temp -> Double.parseDouble(String.format("%.2f", temp)))
                                    .ifPresent(roomVO::setTemperature);

                            DeviceInfo.getValueByName(device.getDeviceInfo(), "humidity", Double.class)
                                    .map(hum -> Double.parseDouble(String.format("%.2f", hum)))
                                    .ifPresent(roomVO::setHumidity);

                            DeviceInfo.getValueByName(device.getDeviceInfo(), "illuminance", Double.class)
                                    .map(ill -> Double.parseDouble(String.format("%.2f", ill)))
                                    .ifPresent(roomVO::setIlluminance);
                            break;

                        case 1: // 8->灯光
                            checkDeviceLightStatus(device, roomVO);
                            break;

                        case 3: // 5->人体感应雷达
                            DeviceInfo.getValueByName(device.getDeviceInfo(), "motion", Integer.class)
                                    .filter(motion -> motion == 1)
                                    .ifPresent(motion -> roomVO.setHuman(true));
                            break;
                    }
                    switch (device.getDeviceTypeId().intValue()) {
                        // 4->计量插座,7->开关,10->智能插座
                        case 4, 8, 10:
                            checkDeviceSwitchStatus(device, roomVO);
                            break;
                    }
                }
            }

        }
    }

    private void checkDeviceLightStatus(DeviceInfoVO device, RoomVO roomVO) {
        DeviceInfo.getValueByName(device.getDeviceInfo(), "count", Integer.class)
                .ifPresent(count -> {
                    for (int i = 0; i < count; i++) {
                        String switchName = "switch" + (i + 1);
                        Optional<String> switchStatus = DeviceInfo.getValueByName(
                                device.getDeviceInfo(),
                                switchName,
                                String.class
                        );
                        if (switchStatus.isPresent() && "ON".equals(switchStatus.get())) {
                            //有灯
                            roomVO.setLight(true);
                            //开
                            roomVO.setIsOpen(true);
                            //灯光路数
                            roomVO.setLightNum(roomVO.getLightNum() + 1); // 设置开启数量
                        }
                    }
                });
    }

    // 检查设备开关状态的通用方法
    private void checkDeviceSwitchStatus(DeviceInfoVO device, RoomVO roomVO) {

        DeviceInfo.getValueByName(device.getDeviceInfo(), "count", Integer.class)
                .ifPresent(count -> {

                    for (int i = 0; i < count; i++) {
                        String switchName = "switch" + (i + 1);
                        Optional<String> switchStatus = DeviceInfo.getValueByName(
                                device.getDeviceInfo(),
                                switchName,
                                String.class
                        );
                        if (switchStatus.isPresent() && "ON".equals(switchStatus.get())) {
                            //有插座
                            roomVO.setPlug(true);
                            //开
                            roomVO.setIsOpen(true);
                            //插座路数
                            roomVO.setPlugNum(roomVO.getPlugNum() + 1); // 设置开启数量
                        }
                    }
                });
    }


    @Operation(summary = "房间下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listRoomOptions() {
        List<Option<Long>> list = roomService.listRoomOptions();
        return Result.success(list);
    }

    @Operation(summary = "新增房间管理")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('room:room:add')")
    public Result<Void> saveRoom(@RequestBody @Valid RoomForm formData) {
        boolean result = roomService.saveRoom(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取房间管理表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('room:room:edit')")
    public Result<RoomForm> getRoomForm(@Parameter(description = "房间管理ID") @PathVariable Long id) {
        RoomForm formData = roomService.getRoomFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改房间管理")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('room:room:edit')")
    public Result<Void> updateRoom(@Parameter(description = "房间管理ID") @PathVariable Long id, @RequestBody @Validated RoomForm formData) {
        boolean result = roomService.updateRoom(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除房间管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('room:room:delete')")
    public Result<Void> deleteRooms(@Parameter(description = "房间管理ID，多个以英文逗号(,)分割") @PathVariable String ids) {
        Long devicesCount = deviceService.listDevicesCount("room", ids);
        if (devicesCount != 0) {
            return Result.failed("该房间下有设备，请先删除设备");
        }
        boolean result = roomService.deleteRooms(ids);
        return Result.judge(result);
    }

    @Operation(summary = "房间设备信息")
    @GetMapping("/device/{id}")
    public Result<List<DeviceInfoVO>> getRoomDetail(@Parameter(description = "房间管理ID") @PathVariable Long id) {
        //查询房间是否存在
        Room room = roomService.getById(id);
        if (ObjectUtils.isEmpty(room)) return Result.failed("房间不存在");
        //根据房间id查询设备
        List<DeviceInfoVO> roomDevices = deviceService.listDeviceByRoomId(id, room);
        return Result.success(roomDevices);
    }
}
