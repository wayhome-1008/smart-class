package com.youlai.boot.deviceType.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.deviceType.model.query.DeviceTypeQuery;
import com.youlai.boot.deviceType.model.vo.DeviceTypeVO;
import com.youlai.boot.deviceType.service.DeviceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备类型字典(自维护)前端控制层
 *
 * @author way
 * @since 2025-05-19 10:59
 */
@Tag(name = "设备类型字典(自维护)接口")
@RestController
@RequestMapping("/api/v1/deviceType")
@RequiredArgsConstructor
public class DeviceTypeController {

    private final DeviceTypeService deviceTypeService;

    @Operation(summary = "设备类型字分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('deviceType:deviceType:query')")
    public PageResult<DeviceTypeVO> getDeviceTypePage(DeviceTypeQuery queryParams) {
        IPage<DeviceTypeVO> result = deviceTypeService.getDeviceTypePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "房间下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listRoomOptions() {
        List<Option<Long>> list = deviceTypeService.listDeviceTypeOptions();
        return Result.success(list);
    }
}
