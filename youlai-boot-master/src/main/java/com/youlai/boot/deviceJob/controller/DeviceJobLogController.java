package com.youlai.boot.deviceJob.controller;

import com.youlai.boot.deviceJob.service.DeviceJobLogService;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.deviceJob.model.form.DeviceJobLogForm;
import com.youlai.boot.deviceJob.model.query.DeviceJobLogQuery;
import com.youlai.boot.deviceJob.model.vo.DeviceJobLogVO;
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
 * 任务日志前端控制层
 *
 * @author way
 * @since 2025-07-25 10:58
 */
@Tag(name = "任务日志接口")
@RestController
@RequestMapping("/api/v1/device-job-log")
@RequiredArgsConstructor
public class DeviceJobLogController  {

    private final DeviceJobLogService deviceJobLogService;

    @Operation(summary = "任务日志分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('deviceJobLog:deviceJobLog:query')")
    public PageResult<DeviceJobLogVO> getDeviceJobLogPage(DeviceJobLogQuery queryParams ) {
        IPage<DeviceJobLogVO> result = deviceJobLogService.getDeviceJobLogPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "获取任务日志表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('deviceJobLog:deviceJobLog:edit')")
    public Result<DeviceJobLogForm> getDeviceJobLogForm(
        @Parameter(description = "任务日志ID") @PathVariable Long id
    ) {
        DeviceJobLogForm formData = deviceJobLogService.getDeviceJobLogFormData(id);
        return Result.success(formData);
    }

}
