package com.youlai.boot.alertEvent.controller;

import com.youlai.boot.alertEvent.service.AlertEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.alertEvent.model.form.AlertEventForm;
import com.youlai.boot.alertEvent.model.query.AlertEventQuery;
import com.youlai.boot.alertEvent.model.vo.AlertEventVO;
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
 * 报警记录前端控制层
 *
 * @author way
 * @since 2025-07-21 12:16
 */
@Tag(name = "报警记录接口")
@RestController
@RequestMapping("/api/v1/alert-event")
@RequiredArgsConstructor
public class AlertEventController  {

    private final AlertEventService alertEventService;

    @Operation(summary = "报警记录分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('alertEvent:alert-event:query')")
    public PageResult<AlertEventVO> getAlertEventPage(AlertEventQuery queryParams ) {
        IPage<AlertEventVO> result = alertEventService.getAlertEventPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增报警记录")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('alertEvent:alert-event:add')")
    public Result<Void> saveAlertEvent(@RequestBody @Valid AlertEventForm formData ) {
        boolean result = alertEventService.saveAlertEvent(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取报警记录表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('alertEvent:alert-event:edit')")
    public Result<AlertEventForm> getAlertEventForm(
        @Parameter(description = "报警记录ID") @PathVariable Long id
    ) {
        AlertEventForm formData = alertEventService.getAlertEventFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改报警记录")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('alertEvent:alert-event:edit')")
    public Result<Void> updateAlertEvent(
            @Parameter(description = "报警记录ID") @PathVariable Long id,
            @RequestBody @Validated AlertEventForm formData
    ) {
        boolean result = alertEventService.updateAlertEvent(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除报警记录")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('alertEvent:alert-event:delete')")
    public Result<Void> deleteAlertEvents(
        @Parameter(description = "报警记录ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = alertEventService.deleteAlertEvents(ids);
        return Result.judge(result);
    }
}
