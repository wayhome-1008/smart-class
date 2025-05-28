package com.youlai.boot.building.controller;

import com.youlai.boot.building.service.BuildingService;
import com.youlai.boot.common.model.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.building.model.form.BuildingForm;
import com.youlai.boot.building.model.query.BuildingQuery;
import com.youlai.boot.building.model.vo.BuildingVO;
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
 * 教学楼管理前端控制层
 *
 * @author way
 * @since 2025-05-08 14:00
 */
@Tag(name = "02.教学楼管理接口")
@RestController
@RequestMapping("/api/v1/building")
@RequiredArgsConstructor
public class BuildingController  {

    private final BuildingService buildingService;

    @Operation(summary = "教学楼管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('building:building:query')")
    public PageResult<BuildingVO> getBuildingPage(BuildingQuery queryParams ) {
        IPage<BuildingVO> result = buildingService.getBuildingPage(queryParams);
        return PageResult.success(result);
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
    public Result<Void> saveBuilding(@RequestBody @Valid BuildingForm formData ) {
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
    public Result<Void> deleteBuildings(
        @Parameter(description = "教学楼管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = buildingService.deleteBuildings(ids);
        return Result.judge(result);
    }
}
