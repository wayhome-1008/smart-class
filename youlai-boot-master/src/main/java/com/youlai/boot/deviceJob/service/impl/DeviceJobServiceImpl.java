package com.youlai.boot.deviceJob.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.deviceJob.converter.DeviceJobConverter;
import com.youlai.boot.deviceJob.job.DeviceSyncJob;
import com.youlai.boot.deviceJob.mapper.DeviceJobMapper;
import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import com.youlai.boot.deviceJob.model.form.DeviceJobForm;
import com.youlai.boot.deviceJob.model.query.DeviceJobQuery;
import com.youlai.boot.deviceJob.model.vo.DeviceJobVO;
import com.youlai.boot.deviceJob.service.DeviceJobService;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 任务管理服务实现类
 *
 * @author way
 * @since 2025-06-30 18:27
 */
@Service
@RequiredArgsConstructor
public class DeviceJobServiceImpl extends ServiceImpl<DeviceJobMapper, DeviceJob> implements DeviceJobService {
    private Scheduler scheduler;
    private final DeviceJobConverter deviceJobConverter;

    /**
     * 获取任务管理分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<DeviceJobVO>} 任务管理分页列表
     */
    @Override
    public IPage<DeviceJobVO> getDeviceJobPage(DeviceJobQuery queryParams) {
        Page<DeviceJobVO> pageVO = this.baseMapper.getDeviceJobPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }

    /**
     * 获取任务管理表单数据
     *
     * @param id 任务管理ID
     * @return 任务管理表单数据
     */
    @Override
    public DeviceJobForm getDeviceJobFormData(Long id) {
        DeviceJob entity = this.getById(id);
        return deviceJobConverter.toForm(entity);
    }

    /**
     * 新增任务管理
     *
     * @param formData 任务管理表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveDeviceJob(DeviceJobForm formData) throws SchedulerException {
        String jobClass = "com.youlai.boot.deviceJob.job.DeviceSyncJob";
        // 验证Cron表达式
        if (!checkCronExpression(formData.getCron())) {
            throw new SchedulerException("无效的Cron表达式: " + formData.getCron());
        }

        // 创建JobDetail
        JobDetail jobDetail = JobBuilder.newJob(DeviceSyncJob.class)
                .withIdentity(formData.getJobName(), formData.getJobGroup())
                .withDescription(formData.getRemark())
                .build();

        // 设置任务数据
        jobDetail.getJobDataMap().put("deviceId", formData.getDeviceId());
        //后续如果deviceId不满足的话 可以再添加新的数据在这里
//        if (deviceJob.getJobData() != null) {
//            jobDetail.getJobDataMap().put("jobData", formData.getJobData());
//        }

        // 创建Trigger
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(formData.getJobName(), formData.getJobGroup())
                .withSchedule(CronScheduleBuilder.cronSchedule(formData.getCron()))
                .build();

        // 注册任务和触发器
        scheduler.scheduleJob(jobDetail, trigger);
        DeviceJob entity = deviceJobConverter.toEntity(formData);
        // 1. 自动生成jobName（设备ID+时间戳）
        String jobName = generateUniqueJobName(String.valueOf(formData.getDeviceId()));
        // 2. 自动生成jobGroup（根据设备类型）
        String jobGroup = determineJobGroup(String.valueOf(formData.getTypeId()));
        entity.setJobName(jobName);
        entity.setJobGroup(jobGroup);
        entity.setJobClass(jobClass);
        return this.save(entity);
    }

    /**
     * 更新任务管理
     *
     * @param id   任务管理ID
     * @param formData 任务管理表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateDeviceJob(Long id, DeviceJobForm formData) throws SchedulerException {
        DeviceJob entity = deviceJobConverter.toEntity(formData);
        // 验证Cron表达式
        if (!checkCronExpression(entity.getCron())) {
            throw new SchedulerException("无效的Cron表达式: " + entity.getCron());
        }

        // 获取原始任务
        DeviceJob originalJob = this.getById(id);
        if (originalJob == null) {
            throw new SchedulerException("任务不存在，ID: " + id);
        }

        // 停止当前任务
        scheduler.pauseTrigger(TriggerKey.triggerKey(originalJob.getJobName(), originalJob.getJobGroup()));
        scheduler.unscheduleJob(TriggerKey.triggerKey(originalJob.getJobName(), originalJob.getJobGroup()));

        // 创建新的Trigger
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(originalJob.getJobName(), originalJob.getJobGroup())
                .withSchedule(CronScheduleBuilder.cronSchedule(formData.getCron()))
                .build();

        // 重新调度任务
        scheduler.rescheduleJob(TriggerKey.triggerKey(originalJob.getJobName(), originalJob.getJobGroup()), trigger);
        return this.updateById(entity);
    }

    /**
     * 删除任务管理
     *
     * @param ids 任务管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteDeviceJobs(String ids) throws SchedulerException {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的任务管理数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        //全部获取
        List<DeviceJob> jobs = this.listByIds(idList);
        for (DeviceJob job : jobs) {
            // 停止并删除任务
            scheduler.pauseTrigger(TriggerKey.triggerKey(job.getJobName(), job.getJobGroup()));
            scheduler.unscheduleJob(TriggerKey.triggerKey(job.getJobName(), job.getJobGroup()));
            scheduler.deleteJob(JobKey.jobKey(job.getJobName(), job.getJobGroup()));
        }
        return this.removeByIds(idList);
    }

    /**
     * 暂停设备任务
     */
    @Override
    public boolean pauseJob(Long id) throws SchedulerException {
        DeviceJob job = this.getById(id);
        if (job == null) {
            return false;
        }
        // 暂停任务
        scheduler.pauseJob(JobKey.jobKey(job.getJobName(), job.getJobGroup()));
        // 更新状态
        job.setStatus(0); // 0表示暂停
        return this.updateById(job);
    }

    /**
     * 恢复设备任务
     */
    @Override
    public boolean resumeJob(Long id) throws SchedulerException {
        DeviceJob deviceJob = this.getById(id);
        if (deviceJob == null) {
            return false;
        }
        // 恢复任务
        scheduler.resumeJob(JobKey.jobKey(deviceJob.getJobName(), deviceJob.getJobGroup()));

        // 更新状态
        deviceJob.setStatus(1); // 1表示运行中
        return this.updateById(deviceJob);
    }

    /**
     * 立即执行设备任务
     */
    @Override
    public boolean runOnce(Long id) throws SchedulerException {
        DeviceJob deviceJob = this.getById(id);
        if (deviceJob == null) {
            return false;
        }
        // 立即执行一次任务
        scheduler.triggerJob(JobKey.jobKey(deviceJob.getJobName(), deviceJob.getJobGroup()));
        return true;
    }

    @Override
    public boolean checkCronExpression(String cronExpression) {
        try {
            CronScheduleBuilder.cronSchedule(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String generateUniqueJobName(String deviceId) {
        return deviceId + "_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String determineJobGroup(String deviceType) {
        return "DeviceType_" + (deviceType != null ? deviceType : "DEFAULT");
    }

}
