package com.youlai.boot.device.controller;

import com.youlai.boot.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.device.model.form.DeviceForm;
import com.youlai.boot.device.model.query.DeviceQuery;
import com.youlai.boot.device.model.vo.DeviceVO;
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

/**
 * 设备管理前端控制层
 *
 * @author way
 * @since 2025-05-08 15:16
 */
@Tag(name = "设备管理接口")
@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController  {

    private final DeviceService deviceService;

    @Operation(summary = "设备管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('device:device:query')")
    public PageResult<DeviceVO> getDevicePage(DeviceQuery queryParams ) {
        IPage<DeviceVO> result = deviceService.getDevicePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增设备管理")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('device:device:add')")
    public Result<Void> saveDevice(@RequestBody @Valid DeviceForm formData ) {
        boolean result = deviceService.saveDevice(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取设备管理表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('device:device:edit')")
    public Result<DeviceForm> getDeviceForm(
        @Parameter(description = "设备管理ID") @PathVariable Long id
    ) {
        DeviceForm formData = deviceService.getDeviceFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改设备管理")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('device:device:edit')")
    public Result<Void> updateDevice(
            @Parameter(description = "设备管理ID") @PathVariable Long id,
            @RequestBody @Validated DeviceForm formData
    ) {
        boolean result = deviceService.updateDevice(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除设备管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('device:device:delete')")
    public Result<Void> deleteDevices(
        @Parameter(description = "设备管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = deviceService.deleteDevices(ids);
        return Result.judge(result);
    }
}
