package com.youlai.boot.alertEvent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.alertEvent.model.query.AlertEventQuery;
import com.youlai.boot.alertEvent.model.vo.AlertEventVO;
import com.youlai.boot.alertEvent.service.AlertEventService;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
public class AlertEventController {

    private final AlertEventService alertEventService;

    @Operation(summary = "报警记录分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('alertEvent:alertEvent:query')")
    public PageResult<AlertEventVO> getAlertEventPage(AlertEventQuery queryParams) {
        IPage<AlertEventVO> result = alertEventService.getAlertEventPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "批量修改报警记录状态")
    @PostMapping("/status")
    @PreAuthorize("@ss.hasPerm('alertEvent:alertEvent:edit')")
    @Log(value = "批量修改报警记录状态", module = LogModuleEnum.ALERT_EVENT)
    public Result<Void> updateStatus(
            @RequestParam String ids,
            @RequestParam Integer status) {
        boolean result = alertEventService.updateStatus(ids, status);
        return result ? Result.success() : Result.failed("状态更新失败");
    }
}
