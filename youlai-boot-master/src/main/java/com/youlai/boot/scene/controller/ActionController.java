package com.youlai.boot.scene.controller;

import com.youlai.boot.scene.service.ActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.scene.model.form.ActionForm;
import com.youlai.boot.scene.model.query.ActionQuery;
import com.youlai.boot.scene.model.vo.ActionVO;
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
 * 执行器前端控制层
 *
 * @author way
 * @since 2025-07-30 17:28
 */
@Tag(name = "执行器接口")
@RestController
@RequestMapping("/api/v1/action")
@RequiredArgsConstructor
public class ActionController  {

    private final ActionService actionService;

    @Operation(summary = "执行器分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('scene:action:query')")
    public PageResult<ActionVO> getActionPage(ActionQuery queryParams ) {
        IPage<ActionVO> result = actionService.getActionPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增执行器")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('scene:action:add')")
    public Result<Void> saveAction(@RequestBody @Valid ActionForm formData ) {
        boolean result = actionService.saveAction(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取执行器表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('scene:action:edit')")
    public Result<ActionForm> getActionForm(
        @Parameter(description = "执行器ID") @PathVariable Long id
    ) {
        ActionForm formData = actionService.getActionFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改执行器")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('scene:action:edit')")
    public Result<Void> updateAction(
            @Parameter(description = "执行器ID") @PathVariable Long id,
            @RequestBody @Validated ActionForm formData
    ) {
        boolean result = actionService.updateAction(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除执行器")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scene:action:delete')")
    public Result<Void> deleteActions(
        @Parameter(description = "执行器ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = actionService.deleteActions(ids);
        return Result.judge(result);
    }
}
