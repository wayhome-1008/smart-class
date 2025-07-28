package com.youlai.boot.deviceJob.controller;

import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import com.youlai.boot.deviceJob.service.DeviceJobService;
import com.youlai.boot.deviceJob.util.CronUtils;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional(rollbackFor = Exception.class)
public class DeviceJobController {

    private final DeviceJobService deviceJobService;
    private final DeviceService deviceService;

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
        if (!CronUtils.isValid(formData.getCron())) {
            return Result.failed("新增任务失败，Cron表达式不正确");
        }
        Device device = deviceService.getById(formData.getDeviceId());
        if (device == null) {
            return Result.failed("设备不存在");
        }
        formData.setDeviceName(device.getDeviceName());
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
        Device device = deviceService.getById(formData.getDeviceId());
        if (device == null) {
            return Result.failed("设备不存在");
        }
        formData.setDeviceName(device.getDeviceName());
        return Result.success(formData);
    }

    @Operation(summary = "修改任务管理")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('deviceJob:deviceJob:edit')")
    public Result<Void> updateDeviceJob(
            @Parameter(description = "任务管理ID") @PathVariable Long id,
            @RequestBody @Validated DeviceJobForm formData
    ) throws SchedulerException {
        if (!CronUtils.isValid(formData.getCron())) {
            return Result.failed("修改任务失败，Cron表达式不正确");
        }
        Device device = deviceService.getById(formData.getDeviceId());
        if (device == null) {
            return Result.failed("设备不存在");
        }
        formData.setDeviceName(device.getDeviceName());
        boolean result = deviceJobService.updateDeviceJob(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除任务管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('deviceJob:deviceJob:delete')")
    public Result<Void> deleteDeviceJobs(
            @Parameter(description = "任务管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) throws SchedulerException {
        deviceJobService.deleteDeviceJobs(ids);
        return Result.success();
    }

    @Operation(summary = "定时任务状态修改")
    @PostMapping("/changeStatus")
    public Result<Void> changeStatus(@RequestBody @Validated DeviceJobForm formData) throws SchedulerException {
        DeviceJob newJob = deviceJobService.getById(formData.getId());
        newJob.setStatus(formData.getStatus());
        boolean result = deviceJobService.changeStatus(newJob);
        return Result.judge(result);
    }

    @Operation(summary = "立即执行任务")
    @PostMapping("/run")
    public Result<Void> run(@RequestBody @Validated DeviceJobForm formData) throws SchedulerException {
        deviceJobService.run(formData);
        return Result.success();
    }
}
