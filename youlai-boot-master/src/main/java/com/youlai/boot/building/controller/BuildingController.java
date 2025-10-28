package com.youlai.boot.building.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.building.model.form.BuildingForm;
import com.youlai.boot.building.model.query.BuildingQuery;
import com.youlai.boot.building.model.vo.BuildingVO;
import com.youlai.boot.building.service.BuildingService;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.device.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教学楼管理前端控制层
 *
 * @author way
 * @since 2025-05-08 14:00
 */
@Tag(name = "02.教学楼管理接口")
@RestController
@RequestMapping("/api/v1/building")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;
    private final DeviceService deviceService;

    @Operation(summary = "教学楼管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('building:building:query')")
    public PageResult<BuildingVO> getBuildingPage(BuildingQuery queryParams) {
        IPage<BuildingVO> result = buildingService.getBuildingPage(queryParams);
        return PageResult.success(result);
    }

    /**
     * 获取楼宇结构选项列表
     */
    @GetMapping("/struct")
    public Result<List<Option<String>>> buildingStructureOptions() {
        List<Option<String>> options = buildingService.buildingStructureOptions();
        return Result.success(options);
    }

    @Operation(summary = "教学楼下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listBuildingOptions() {
        List<Option<Long>> list = buildingService.listBuildingOptions();
        return Result.success(list);
    }

    @Operation(summary = "新增教学楼管理")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('building:building:add')")
    @Log(value = "新建楼宇", module = LogModuleEnum.BUILDING)
    public Result<Void> saveBuilding(@RequestBody @Valid BuildingForm formData) {
        boolean result = buildingService.saveBuilding(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取教学楼管理表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('building:building:edit')")
    public Result<BuildingForm> getBuildingForm(
            @Parameter(description = "教学楼管理ID") @PathVariable Long id
    ) {
        BuildingForm formData = buildingService.getBuildingFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改教学楼管理")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('building:building:edit')")
    @Log(value = "修改楼宇", module = LogModuleEnum.BUILDING)
    public Result<Void> updateBuilding(
            @Parameter(description = "教学楼管理ID") @PathVariable Long id,
            @RequestBody @Validated BuildingForm formData
    ) {
        boolean result = buildingService.updateBuilding(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除教学楼管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('building:building:delete')")
    @Log(value = "删除楼宇", module = LogModuleEnum.BUILDING)
    public Result<Void> deleteBuildings(
            @Parameter(description = "教学楼管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        Long devicesCount = deviceService.listDevicesCount("building", ids);
        if (devicesCount != 0) {
            return Result.failed("该楼宇下有设备，请先删除设备");
        }
        boolean result = buildingService.deleteBuildings(ids);
        return Result.judge(result);
    }
}
