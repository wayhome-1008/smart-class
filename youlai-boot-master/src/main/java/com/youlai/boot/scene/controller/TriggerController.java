package com.youlai.boot.scene.controller;

import com.youlai.boot.scene.service.TriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.scene.model.form.TriggerForm;
import com.youlai.boot.scene.model.query.TriggerQuery;
import com.youlai.boot.scene.model.vo.TriggerVO;
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
 * 触发器前端控制层
 *
 * @author way
 * @since 2025-07-30 17:25
 */
@Tag(name = "触发器接口")
@RestController
@RequestMapping("/api/v1/trigger")
@RequiredArgsConstructor
public class TriggerController  {

    private final TriggerService triggerService;

    @Operation(summary = "触发器分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('scene:trigger:query')")
    public PageResult<TriggerVO> getTriggerPage(TriggerQuery queryParams ) {
        IPage<TriggerVO> result = triggerService.getTriggerPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增触发器")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('scene:trigger:add')")
    public Result<Void> saveTrigger(@RequestBody @Valid TriggerForm formData ) {
        boolean result = triggerService.saveTrigger(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取触发器表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('scene:trigger:edit')")
    public Result<TriggerForm> getTriggerForm(
        @Parameter(description = "触发器ID") @PathVariable Long id
    ) {
        TriggerForm formData = triggerService.getTriggerFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改触发器")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('scene:trigger:edit')")
    public Result<Void> updateTrigger(
            @Parameter(description = "触发器ID") @PathVariable Long id,
            @RequestBody @Validated TriggerForm formData
    ) {
        boolean result = triggerService.updateTrigger(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除触发器")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scene:trigger:delete')")
    public Result<Void> deleteTriggers(
        @Parameter(description = "触发器ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = triggerService.deleteTriggers(ids);
        return Result.judge(result);
    }
}
