package com.youlai.boot.floor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.floor.model.form.FloorForm;
import com.youlai.boot.floor.model.query.FloorQuery;
import com.youlai.boot.floor.model.vo.FloorVO;
import com.youlai.boot.floor.service.FloorService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.youlai.boot.room.controller.RoomController.checkDeviceSwitchStatus;

/**
 * 楼层管理前端控制层
 *
 * @author way
 * @since 2025-05-08 12:23
 */
@Tag(name = "07.楼层管理接口")
@RestController
@RequestMapping("/api/v1/floor")
@RequiredArgsConstructor
public class FloorController {

    private final FloorService floorService;
    private final DeviceService deviceService;
    private final RoomService roomService;

    @Operation(summary = "楼层管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('floor:floor:query')")
    public PageResult<FloorVO> getFloorPage(FloorQuery queryParams) {
        // 1. 获取楼层分页数据
        IPage<FloorVO> result = floorService.getFloorPage(queryParams);
        if (result.getRecords().isEmpty()) {
            return PageResult.success(result);
        }
        // 2. 获取这些楼层中的所有设备
        List<DeviceInfoVO> devices = deviceService.listDeviceByFloorIds(result.getRecords());
        if (devices.isEmpty()) {
            return PageResult.success(result);
        }
        //3.获取楼层得房间
        List<Room> roomList = roomService.listRoomByFloor(result.getRecords());
        // 4. 构建房间ID到楼层ID的映射
        Map<Long, Long> roomFloorMap = roomList.stream()
                .collect(Collectors.toMap(Room::getId, Room::getFloorId));
        // 5. 按楼层ID分组设备
        Map<Long, List<DeviceInfoVO>> floorDevicesMap = devices.stream()
                .filter(device -> roomFloorMap.containsKey(device.getDeviceRoom()))
                .collect(Collectors.groupingBy(device -> roomFloorMap.get(device.getDeviceRoom())));
        // 6. 将设备列表设置到对应的楼层VO中
        result.getRecords().forEach(floor -> {
            List<DeviceInfoVO> floorDevices = floorDevicesMap.getOrDefault(floor.getId(), new ArrayList<>());
            floor.setDeviceInfo(floorDevices);
            initFloorStatusIndicators(floor, floorDevices);
        });
        return PageResult.success(result);
    }

    private void initFloorStatusIndicators(FloorVO floor, List<DeviceInfoVO> floorDevices) {
        // 默认值设置
        floor.setLight(false);
        floor.setPlug(false);
        floor.setHuman(false);
        floor.setIsOpen(false);
        floor.setLightNum(0);
        floor.setPlugNum(0);
        for (DeviceInfoVO device : floorDevices) {
            switch (device.getDeviceTypeId().intValue()) {
                case 2: // 2->温湿度传感器
                    DeviceInfo.getValueByName(device.getDeviceInfo(), "temperature", Double.class)
                            .ifPresent(floor::setTemperature);
                    DeviceInfo.getValueByName(device.getDeviceInfo(), "humidity", Double.class)
                            .ifPresent(floor::setHumidity);
                    DeviceInfo.getValueByName(device.getDeviceInfo(), "Illuminance", Double.class)
                            .ifPresent(floor::setIlluminance);
                    break;

                case 8: // 8->灯光
                    checkDeviceSwitchStatus(device, "power", floor::setLight, floor::setIsOpen, floor::setLightNum);
                    break;
                case 4:
                case 7:
                case 10: // 4->计量插座,7->开关,10->智能插座
                    checkDeviceSwitchStatus(device, "switch", floor::setPlug, floor::setIsOpen, floor::setPlugNum);
                    break;

                case 5: // 5->人体感应雷达
                    DeviceInfo.getValueByName(device.getDeviceInfo(), "motion", Integer.class)
                            .filter(motion -> motion == 1)
                            .ifPresent(motion -> floor.setHuman(true));
                    break;

                case 6: // 6->人体存在感应
                    DeviceInfo.getValueByName(device.getDeviceInfo(), "Occupancy", Integer.class)
                            .filter(occupancy -> occupancy == 1)
                            .ifPresent(occupancy -> floor.setHuman(true));
                    break;
            }
        }
    }

    @Operation(summary = "新增楼层管理")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('floor:floor:add')")
    public Result<Void> saveFloor(@RequestBody @Valid FloorForm formData) {
        boolean result = floorService.saveFloor(formData);
        return Result.judge(result);
    }

    @Operation(summary = "根据教学楼id查询楼层下拉列表")
    @GetMapping("/building/{buildingId}")
    public Result<List<Option<Long>>> listFloorOptionsByBuildingId(
            @Parameter(description = "教学楼id") @PathVariable Long buildingId
    ) {
        List<Option<Long>> list = floorService.listFloorOptionsByBuildingId(buildingId);
        return Result.success(list);
    }

    @Operation(summary = "获取楼层管理表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('floor:floor:edit')")
    public Result<FloorForm> getFloorForm(
            @Parameter(description = "楼层管理ID") @PathVariable Long id
    ) {
        FloorForm formData = floorService.getFloorFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改楼层管理")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('floor:floor:edit')")
    public Result<Void> updateFloor(
            @Parameter(description = "楼层管理ID") @PathVariable Long id,
            @RequestBody @Validated FloorForm formData
    ) {
        boolean result = floorService.updateFloor(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除楼层管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('floor:floor:delete')")
    public Result<Void> deleteFloors(
            @Parameter(description = "楼层管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        Long devicesCount = deviceService.listDevicesCount("floor", ids);
        if (devicesCount != 0) {
            return Result.failed("该楼层下有设备，请先删除设备");
        }
        boolean result = floorService.deleteFloors(ids);
        return Result.judge(result);
    }
}
