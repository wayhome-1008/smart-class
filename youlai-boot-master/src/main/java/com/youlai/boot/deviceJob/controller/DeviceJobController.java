package com.youlai.boot.deviceJob.controller;

import com.youlai.boot.deviceJob.service.DeviceJobService;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.deviceJob.model.form.DeviceJobForm;
import com.youlai.boot.deviceJob.model.query.DeviceJobQuery;
import com.youlai.boot.deviceJob.model.vo.DeviceJobVO;
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
 * 任务管理前端控制层
 *
 * @author way
 * @since 2025-06-30 18:27
 */
@Tag(name = "任务管理接口")
@RestController
@RequestMapping("/api/v1/device-job")
@RequiredArgsConstructor
public class DeviceJobController {

    private final DeviceJobService deviceJobService;

    @Operation(summary = "任务管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('deviceJob:deviceJob:query')")
    public PageResult<DeviceJobVO> getDeviceJobPage(DeviceJobQuery queryParams) {
        IPage<DeviceJobVO> result = deviceJobService.getDeviceJobPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增任务管理")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('deviceJob:deviceJob:add')")
    public Result<Void> saveDeviceJob(@RequestBody @Valid DeviceJobForm formData) throws SchedulerException {
        boolean result = deviceJobService.saveDeviceJob(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取任务管理表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('deviceJob:deviceJob:edit')")
    public Result<DeviceJobForm> getDeviceJobForm(
            @Parameter(description = "任务管理ID") @PathVariable Long id
    ) {
        DeviceJobForm formData = deviceJobService.getDeviceJobFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改任务管理")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('deviceJob:deviceJob:edit')")
    public Result<Void> updateDeviceJob(
            @Parameter(description = "任务管理ID") @PathVariable Long id,
            @RequestBody @Validated DeviceJobForm formData
    ) throws SchedulerException {
        boolean result = deviceJobService.updateDeviceJob(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除任务管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('deviceJob:deviceJob:delete')")
    public Result<Void> deleteDeviceJobs(
            @Parameter(description = "任务管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) throws SchedulerException {
        boolean result = deviceJobService.deleteDeviceJobs(ids);
        return Result.judge(result);
    }

    @Operation(summary = "暂停任务")
    @PostMapping("/{id}/pause")
    public Result<Void> pause(@PathVariable Long id) throws SchedulerException {
        boolean result = deviceJobService.pauseJob(id);
        return Result.judge(result);
    }

    @Operation(summary = "恢复任务")
    @PostMapping("/{id}/resume")
    public Result<Void> resume(@PathVariable Long id) throws SchedulerException {
        boolean result = deviceJobService.resumeJob(id);
        return Result.judge(result);
    }

    @Operation(summary = "立即执行任务")
    @PostMapping("/{id}/run-once")
    public Result<Void> runOnce(@PathVariable Long id) throws SchedulerException {
        boolean result = deviceJobService.runOnce(id);
        return Result.judge(result);
    }
}
