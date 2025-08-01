package com.youlai.boot.scene.controller;

import com.youlai.boot.scene.service.SceneDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.scene.model.form.SceneDeviceForm;
import com.youlai.boot.scene.model.query.SceneDeviceQuery;
import com.youlai.boot.scene.model.vo.SceneDeviceVO;
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
 * 场景设备前端控制层
 *
 * @author way
 * @since 2025-07-29 12:03
 */
@Tag(name = "场景设备接口")
@RestController
@RequestMapping("/api/v1/scene-device")
@RequiredArgsConstructor
public class SceneDeviceController  {

    private final SceneDeviceService sceneDeviceService;

    @Operation(summary = "场景设备分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('scene:scene-device:query')")
    public PageResult<SceneDeviceVO> getSceneDevicePage(SceneDeviceQuery queryParams ) {
        IPage<SceneDeviceVO> result = sceneDeviceService.getSceneDevicePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增场景设备")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('scene:scene-device:add')")
    public Result<Void> saveSceneDevice(@RequestBody @Valid SceneDeviceForm formData ) {
        boolean result = sceneDeviceService.saveSceneDevice(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取场景设备表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('scene:scene-device:edit')")
    public Result<SceneDeviceForm> getSceneDeviceForm(
        @Parameter(description = "场景设备ID") @PathVariable Long id
    ) {
        SceneDeviceForm formData = sceneDeviceService.getSceneDeviceFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改场景设备")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('scene:scene-device:edit')")
    public Result<Void> updateSceneDevice(
            @Parameter(description = "场景设备ID") @PathVariable Long id,
            @RequestBody @Validated SceneDeviceForm formData
    ) {
        boolean result = sceneDeviceService.updateSceneDevice(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除场景设备")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scene:scene-device:delete')")
    public Result<Void> deleteSceneDevices(
        @Parameter(description = "场景设备ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = sceneDeviceService.deleteSceneDevices(ids);
        return Result.judge(result);
    }
}
