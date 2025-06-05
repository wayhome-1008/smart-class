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

    @Operation(summary = "房间管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('room:room:query')")
    public PageResult<RoomVO> getRoomPage(RoomQuery queryParams) {
        IPage<RoomVO> result = roomService.getRoomPage(queryParams);
        return PageResult.success(result);
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
    public Result<RoomForm> getRoomForm(
            @Parameter(description = "房间管理ID") @PathVariable Long id
    ) {
        RoomForm formData = roomService.getRoomFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改房间管理")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('room:room:edit')")
    public Result<Void> updateRoom(
            @Parameter(description = "房间管理ID") @PathVariable Long id,
            @RequestBody @Validated RoomForm formData
    ) {
        boolean result = roomService.updateRoom(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除房间管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('room:room:delete')")
    public Result<Void> deleteRooms(
            @Parameter(description = "房间管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = roomService.deleteRooms(ids);
        return Result.judge(result);
    }

    @Operation(summary = "房间设备信息")
    @GetMapping("/device/{id}")
    public Result<List<DeviceInfoVO>> getRoomDetail(
            @Parameter(description = "房间管理ID") @PathVariable Long id
    ) {
        //查询房间是否存在
        Room room = roomService.getById(id);
        if (ObjectUtils.isEmpty(room)) return Result.failed("房间不存在");
        //根据房间id查询设备
        List<Device> roomDevices = deviceService.listDeviceByRoomId(id);
        List<DeviceInfoVO> deviceInfoVOS = new ArrayList<>();
        for (Device roomDevice : roomDevices) {
            //转VO
            DeviceInfoVO deviceInfoVO = basicPropertyConvert(roomDevice, room);
            String deviceType = DeviceTypeEnum.getNameById(roomDevice.getDeviceTypeId());
            String communicationMode = CommunicationModeEnum.getNameById(roomDevice.getCommunicationModeItemId());
            if (!deviceType.equals("Gateway")) {
                DeviceInfoParser parser = DeviceInfoParserFactory.getParser(deviceType, communicationMode);
                List<DeviceInfo> deviceInfos = parser.parse(roomDevice.getDeviceInfo());
                deviceInfoVO.setDeviceInfo(deviceInfos);
            }
            deviceInfoVOS.add(deviceInfoVO);
        }
        return Result.success(deviceInfoVOS);
    }
}
