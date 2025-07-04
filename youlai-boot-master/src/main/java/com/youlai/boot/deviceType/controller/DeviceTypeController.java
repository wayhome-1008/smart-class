package com.youlai.boot.deviceType.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.deviceType.model.query.DeviceTypeQuery;
import com.youlai.boot.deviceType.model.vo.DeviceTypeVO;
import com.youlai.boot.deviceType.service.DeviceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备类型字典(自维护)前端控制层
 *
 * @author way
 * @since 2025-05-19 10:59
 */
@Tag(name = "06.设备类型接口")
@RestController
@RequestMapping("/api/v1/deviceType")
@RequiredArgsConstructor
public class DeviceTypeController {
    private final DeviceTypeService deviceTypeService;
    private final DeviceService deviceService;

    @Operation(summary = "设备类型字分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('deviceType:deviceType:query')")
    public PageResult<DeviceTypeVO> getDeviceTypePage(DeviceTypeQuery queryParams) {
        IPage<DeviceTypeVO> result = deviceTypeService.getDeviceTypePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "设备类型下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listDeviceTypeOptions(@RequestParam(required = true) Boolean showGateway) {
        List<Option<Long>> list = deviceTypeService.listDeviceTypeOptions();
        if (!showGateway) {
            //删除type==1的数据
            list.removeIf(item -> item.getValue()==1);
        }
        return Result.success(list);

    }

    @Operation(summary = "设备类型下拉列表根据房间id过滤")
    @GetMapping("/options/{id}")
    public Result<List<Option<Long>>> listDeviceTypeOptions(@Parameter(description = "房间ID") @PathVariable Long id) {
        List<Option<Long>> allDeviceTypes = deviceTypeService.listDeviceTypeOptions();
        // 查询房间内设备的设备类型ids
        List<Long> roomDeviceTypeIds = deviceService.listRoomDevicesIds(id);

        // 筛选出房间内设备对应的设备类型
        List<Option<Long>> filteredDeviceTypes = allDeviceTypes.stream()
                .filter(option -> roomDeviceTypeIds.contains(option.getValue()))
                .collect(Collectors.toList());

        return Result.success(filteredDeviceTypes);
    }
}
