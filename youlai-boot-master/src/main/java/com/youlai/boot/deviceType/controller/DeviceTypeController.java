package com.youlai.boot.deviceType.controller;

import com.youlai.boot.common.model.Option;
import com.youlai.boot.deviceType.service.DeviceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.deviceType.model.form.DeviceTypeForm;
import com.youlai.boot.deviceType.model.query.DeviceTypeQuery;
import com.youlai.boot.deviceType.model.vo.DeviceTypeVO;
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
public class DeviceTypeController  {

    private final DeviceTypeService deviceTypeService;

    @Operation(summary = "设备类型字分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('deviceType:deviceType:query')")
    public PageResult<DeviceTypeVO> getDeviceTypePage(DeviceTypeQuery queryParams ) {
        IPage<DeviceTypeVO> result = deviceTypeService.getDeviceTypePage(queryParams);
        return PageResult.success(result);
    }

//    @Operation(summary = "新增设备类型字典(自维护)")
//    @PostMapping
//    @PreAuthorize("@ss.hasPerm('deviceType:deviceType:add')")
//    public Result<Void> saveDeviceType(@RequestBody @Valid DeviceTypeForm formData ) {
//        boolean result = deviceTypeService.saveDeviceType(formData);
//        return Result.judge(result);
//    }

//    @Operation(summary = "获取设备类型字典(自维护)表单数据")
//    @GetMapping("/{id}/form")
//    @PreAuthorize("@ss.hasPerm('deviceType:deviceType:edit')")
//    public Result<DeviceTypeForm> getDeviceTypeForm(
//        @Parameter(description = "设备类型字典(自维护)ID") @PathVariable Long id
//    ) {
//        DeviceTypeForm formData = deviceTypeService.getDeviceTypeFormData(id);
//        return Result.success(formData);
//    }

//    @Operation(summary = "修改设备类型字典(自维护)")
//    @PutMapping(value = "/{id}")
//    @PreAuthorize("@ss.hasPerm('deviceType:deviceType:edit')")
//    public Result<Void> updateDeviceType(
//            @Parameter(description = "设备类型字典(自维护)ID") @PathVariable Long id,
//            @RequestBody @Validated DeviceTypeForm formData
//    ) {
//        boolean result = deviceTypeService.updateDeviceType(id, formData);
//        return Result.judge(result);
//    }
//
//    @Operation(summary = "删除设备类型字典(自维护)")
//    @DeleteMapping("/{ids}")
//    @PreAuthorize("@ss.hasPerm('deviceType:deviceType:delete')")
//    public Result<Void> deleteDeviceTypes(
//        @Parameter(description = "设备类型字典(自维护)ID，多个以英文逗号(,)分割") @PathVariable String ids
//    ) {
//        boolean result = deviceTypeService.deleteDeviceTypes(ids);
//        return Result.judge(result);
//    }

    @Operation(summary = "房间下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listRoomOptions() {
        List<Option<Long>> list = deviceTypeService.listDeviceTypeOptions();
        return Result.success(list);
    }
}
