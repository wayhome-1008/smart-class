package com.youlai.boot.floor.controller;

import com.youlai.boot.common.model.Option;
import com.youlai.boot.floor.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.floor.model.form.FloorForm;
import com.youlai.boot.floor.model.query.FloorQuery;
import com.youlai.boot.floor.model.vo.FloorVO;
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
 * 楼层管理前端控制层
 *
 * @author way
 * @since 2025-05-08 12:23
 */
@Tag(name = "楼层管理接口")
@RestController
@RequestMapping("/api/v1/floor")
@RequiredArgsConstructor
public class FloorController  {

    private final FloorService floorService;

    @Operation(summary = "楼层管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('floor:floor:query')")
    public PageResult<FloorVO> getFloorPage(FloorQuery queryParams ) {
        IPage<FloorVO> result = floorService.getFloorPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增楼层管理")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('floor:floor:add')")
    public Result<Void> saveFloor(@RequestBody @Valid FloorForm formData ) {
        boolean result = floorService.saveFloor(formData);
        return Result.judge(result);
    }

    @Operation(summary = "根据教学楼id查询楼层下拉列表")
    @GetMapping("/building/{buildingId}")
    public  Result<List<Option<Long>>> listFloorOptionsByBuildingId(
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
        boolean result = floorService.deleteFloors(ids);
        return Result.judge(result);
    }
}
