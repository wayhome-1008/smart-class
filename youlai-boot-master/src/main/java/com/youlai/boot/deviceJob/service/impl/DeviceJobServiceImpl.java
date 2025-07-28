package com.youlai.boot.deviceJob.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.constant.ScheduleConstants;
import com.youlai.boot.deviceJob.converter.DeviceJobConverter;
import com.youlai.boot.deviceJob.mapper.DeviceJobMapper;
import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import com.youlai.boot.deviceJob.model.form.DeviceJobForm;
import com.youlai.boot.deviceJob.model.query.DeviceJobQuery;
import com.youlai.boot.deviceJob.model.vo.DeviceJobVO;
import com.youlai.boot.deviceJob.service.DeviceJobService;
import com.youlai.boot.deviceJob.util.ScheduleUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final Scheduler scheduler;
    private final DeviceJobConverter deviceJobConverter;

    @PostConstruct
    public void init() throws SchedulerException {
        scheduler.clear();
        // 设备定时任务
        List<DeviceJob> jobList = this.list();
        for (DeviceJob deviceJob : jobList) {
            ScheduleUtils.createScheduleJob(scheduler, deviceJob);
        }
    }

    /**
     * 获取任务管理分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<DeviceJobVO>} 任务管理分页列表
     */
    @Override
    public IPage<DeviceJobVO> getDeviceJobPage(DeviceJobQuery queryParams) {
        return this.baseMapper.getDeviceJobPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
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
        DeviceJob deviceJob = deviceJobConverter.toEntity(formData);
        boolean save = this.save(deviceJob);
        if (save) {
            ScheduleUtils.createScheduleJob(scheduler, deviceJob);
        }
        return save;
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

        // 获取原始任务
        DeviceJob originalJob = this.getById(id);
        if (originalJob == null) {
            throw new SchedulerException("任务不存在，ID: " + id);
        }
        boolean updated = this.updateById(entity);
        if (updated) {
            updateSchedulerJob(entity, entity.getJobGroup());
        }
        return updated;
    }

    /**
     * 删除任务管理
     *
     * @param ids 任务管理ID，多个以英文逗号(,)分割
     */
    @Override
    public void deleteDeviceJobs(String ids) throws SchedulerException {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的任务管理数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        //全部获取
        List<DeviceJob> jobs = this.listByIds(idList);
        for (DeviceJob job : jobs) {
            deleteJob(job);
        }
    }

    @Override
    public boolean changeStatus(DeviceJob newJob) throws SchedulerException {
        boolean result = false;
        Integer status = newJob.getStatus();
        if (ScheduleConstants.Status.NORMAL.getValue().equals(String.valueOf(status))) {
            result = resumeJob(newJob);
        } else if (ScheduleConstants.Status.PAUSE.getValue().equals(String.valueOf(status))) {
            result = pauseJob(newJob);
        }
        return result;
    }

    @Override
    public void run(DeviceJobForm formData) throws SchedulerException {
        Long jobId = formData.getId();
        String jobGroup = formData.getJobGroup();
        DeviceJob properties = this.getById(formData.getId());
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, properties);
        scheduler.triggerJob(ScheduleUtils.getJobKey(jobId, jobGroup), dataMap);
    }

    public boolean pauseJob(DeviceJob job) throws SchedulerException {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();
        job.setStatus(Integer.valueOf(ScheduleConstants.Status.PAUSE.getValue()));
        boolean updated = this.updateById(job);
        if (updated) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return updated;
    }

    public Boolean resumeJob(DeviceJob job) throws SchedulerException {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();
        job.setStatus(Integer.valueOf(ScheduleConstants.Status.NORMAL.getValue()));
        boolean updated = this.updateById(job);
        if (updated) {
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return updated;
    }

    /**
     * 删除任务后，所对应的trigger也将被删除
     *
     * @param job 调度信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(DeviceJob job) throws SchedulerException {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();
        boolean removed = this.removeById(jobId);
        if (removed) {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
    }

    /**
     * 更新任务
     *
     * @param deviceJob 任务对象
     * @param jobGroup 任务组名
     */
    public void updateSchedulerJob(DeviceJob deviceJob, String jobGroup) throws SchedulerException {
        Long jobId = deviceJob.getId();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }
        ScheduleUtils.createScheduleJob(scheduler, deviceJob);
    }

}
