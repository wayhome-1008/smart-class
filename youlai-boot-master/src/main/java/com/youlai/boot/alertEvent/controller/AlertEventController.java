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
public class AlertEventController {

    private final AlertEventService alertEventService;

    @Operation(summary = "报警记录分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('alertEvent:alertEvent:query')")
    public PageResult<AlertEventVO> getAlertEventPage(AlertEventQuery queryParams) {
        IPage<AlertEventVO> result = alertEventService.getAlertEventPage(queryParams);
        return PageResult.success(result);
    }

}
