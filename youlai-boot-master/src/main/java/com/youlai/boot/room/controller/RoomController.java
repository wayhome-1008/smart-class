package com.youlai.boot.room.controller;

import com.youlai.boot.common.model.Option;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.Enum.DeviceTypeEnum;
import com.youlai.boot.device.factory.DeviceInfoParserFactory;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.service.DeviceInfoParser;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.room.model.form.RoomForm;
import com.youlai.boot.room.model.query.RoomQuery;
import com.youlai.boot.room.model.vo.RoomVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.youlai.boot.dashBoard.controller.DashBoardController.basicPropertyConvert;

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

//    @Operation(summary = "房间管理分页列表")
//    @GetMapping("/page")
//    @PreAuthorize("@ss.hasPerm('room:room:query')")
//    public PageResult<RoomVO> getRoomPage(RoomQuery queryParams) {
//        // 1. 获取房间分页数据
//        IPage<RoomVO> result = roomService.getRoomPage(queryParams);
//        // 2. 为每个房间添加设备信息
//        List<DeviceInfoVO> devices = deviceService.listDeviceByRoomIds(result.getRecords());
//        for (RoomVO record : result.getRecords()) {
//            List<DeviceInfoVO> roomDevices = new ArrayList<>();
//            for (DeviceInfoVO device : devices) {
//                if (device.getDeviceRoom().equals(record.getId())) {
//                    roomDevices.add(device);
//                    record.setDeviceInfo(roomDevices);
//                    //对每个房间VO判断是否显温度、湿度、光照、是否开灯、是否开插座、是否有人
//                    if (device.getDeviceTypeId() == 2) {
//                        Optional<Double> temperature = DeviceInfo.getValueByName(device.getDeviceInfo(), "temperature", Double.class);
//                        temperature.ifPresent(record::setTemperature);
//                        Optional<Double> humidity = DeviceInfo.getValueByName(device.getDeviceInfo(), "humidity", Double.class);
//                        humidity.ifPresent(record::setHumidity);
//                        Optional<Double> Illuminance = DeviceInfo.getValueByName(device.getDeviceInfo(), "Illuminance", Double.class);
//                        Illuminance.ifPresent(record::setIlluminance);
//                    }
//                    if (device.getDeviceTypeId() == 8) {
//                        //获取开关状态
//                        Optional<Integer> count = DeviceInfo.getValueByName(device.getDeviceInfo(), "count", Integer.class);
//                        if (count.isPresent()) {
//                            boolean found = false;
//                            for (int i = 0; i < count.get() && !found; i++) {
//                                Optional<String> switchStatus = DeviceInfo.getValueByName(
//                                        device.getDeviceInfo(),
//                                        "power" + (i + 1),
//                                        String.class
//                                );
//                                if (switchStatus.isPresent() && switchStatus.get().equals("ON")) {
//                                    record.setLight(true);
//                                    found = true; // 找到ON状态后立即跳出循环
//                                }
//                            }
//                            // 如果没有找到ON状态，设置为false
//                            if (!found) {
//                                record.setLight(false);
//                            }
//                        }
//                    }
//                    if (device.getDeviceTypeId() == 4 || device.getDeviceTypeId() == 7 || device.getDeviceTypeId() == 10) {
//                        //获取开关状态
//                        Optional<Integer> count = DeviceInfo.getValueByName(device.getDeviceInfo(), "count", Integer.class);
//                        if (count.isPresent()) {
//                            boolean found = false;
//                            for (int i = 0; i < count.get() && !found; i++) {
//                                Optional<String> switchStatus = DeviceInfo.getValueByName(
//                                        device.getDeviceInfo(),
//                                        "switch" + (i + 1),
//                                        String.class
//                                );
//                                if (switchStatus.isPresent() && switchStatus.get().equals("ON")) {
//                                    record.setPlug(true);
//                                    found = true; // 找到ON状态后立即跳出循环
//                                }
//                            }
//                            // 如果没有找到ON状态，设置为false
//                            if (!found) {
//                                record.setPlug(false);
//                            }
//                        }
//                    }
//                    //5->人体感应雷达 motion
//                    if (device.getDeviceTypeId() == 5) {
//                        //获取开关状态
//                        Optional<Integer> motion = DeviceInfo.getValueByName(device.getDeviceInfo(), "motion", Integer.class);
//                        if (motion.isPresent()) {
//                            boolean found = false;
//                            if (motion.get() == 1) {
//                                record.setHuman(true);
//                                found = true; // 找到ON状态后立即跳出循环
//                            }
//                            // 如果没有找到ON状态，设置为false
//                            if (!found) {
//                                record.setHuman(false);
//                            }
//                        }
//                    }
//                    if (device.getDeviceTypeId() == 6) {
//                        //获取开关状态
//                        Optional<Integer> occupancy = DeviceInfo.getValueByName(device.getDeviceInfo(), "Occupancy", Integer.class);
//                        if (occupancy.isPresent()) {
//                            boolean found = false;
//                            if (occupancy.get() == 1) {
//                                record.setHuman(true);
//                                found = true; // 找到ON状态后立即跳出循环
//                            }
//                            // 如果没有找到ON状态，设置为false
//                            if (!found) {
//                                record.setHuman(false);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        return PageResult.success(result);
//    }

    @Operation(summary = "房间管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('room:room:query')")
    public PageResult<RoomVO> getRoomPage(RoomQuery queryParams) {
        // 1. 获取房间分页数据
        IPage<RoomVO> result = roomService.getRoomPage(queryParams);

        // 2. 批量获取所有相关设备
        List<DeviceInfoVO> devices = deviceService.listDeviceByRoomIds(result.getRecords());

        // 3. 按房间ID分组设备
        Map<Long, List<DeviceInfoVO>> deviceMap = devices.stream()
                .collect(Collectors.groupingBy(DeviceInfoVO::getDeviceRoom));

        // 4. 为每个房间设置设备及状态
        result.getRecords().forEach(roomVO -> {
            List<DeviceInfoVO> roomDevices = deviceMap.getOrDefault(roomVO.getId(), new ArrayList<>());
            roomVO.setDeviceInfo(roomDevices); // 一次性设置完整设备列表
            // 初始化房间状态指标
            initRoomStatusIndicators(roomVO, roomDevices);
        });

        return PageResult.success(result);
    }

    // 初始化房间状态指标的方法
    private void initRoomStatusIndicators(RoomVO roomVO, List<DeviceInfoVO> devices) {
        // 默认值设置
        roomVO.setLight(false);
        roomVO.setPlug(false);
        roomVO.setHuman(false);

        for (DeviceInfoVO device : devices) {
            switch (device.getDeviceTypeId().intValue()) {
                case 2: // 2->温湿度传感器
                    DeviceInfo.getValueByName(device.getDeviceInfo(), "temperature", Double.class)
                            .ifPresent(roomVO::setTemperature);
                    DeviceInfo.getValueByName(device.getDeviceInfo(), "humidity", Double.class)
                            .ifPresent(roomVO::setHumidity);
                    DeviceInfo.getValueByName(device.getDeviceInfo(), "Illuminance", Double.class)
                            .ifPresent(roomVO::setIlluminance);
                    break;

                case 8: // 8->灯光
                    checkDeviceSwitchStatus(device, "power", roomVO::setLight,roomVO::setIsOpen);
                    break;
                case 4:
                case 7:
                case 10: // 4->计量插座,7->开关,10->智能插座
                    checkDeviceSwitchStatus(device, "switch", roomVO::setPlug,roomVO::setIsOpen);
                    break;

                case 5: // 5->人体感应雷达
                    DeviceInfo.getValueByName(device.getDeviceInfo(), "motion", Integer.class)
                            .filter(motion -> motion == 1)
                            .ifPresent(motion -> roomVO.setHuman(true));
                    break;

                case 6: // 6->人体存在感应
                    DeviceInfo.getValueByName(device.getDeviceInfo(), "Occupancy", Integer.class)
                            .filter(occupancy -> occupancy == 1)
                            .ifPresent(occupancy -> roomVO.setHuman(true));
                    break;
            }
        }
    }

    // 检查设备开关状态的通用方法
    public static void checkDeviceSwitchStatus(DeviceInfoVO device, String switchPrefix, Consumer<Boolean> statusSetter, Consumer<Boolean> openSetter) {
        DeviceInfo.getValueByName(device.getDeviceInfo(), "count", Integer.class)
                .ifPresent(count -> {
                    for (int i = 0; i < count; i++) {
                        String switchName = switchPrefix + (i + 1);
                        Optional<String> switchStatus = DeviceInfo.getValueByName(
                                device.getDeviceInfo(),
                                switchName,
                                String.class
                        );
                        if (switchStatus.isPresent() && "ON".equals(switchStatus.get())) {
                            statusSetter.accept(true);
                            openSetter.accept(true);
                            return; // 找到ON状态后立即返回
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
