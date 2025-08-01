package com.youlai.boot.deviceJob.util;

import com.youlai.boot.common.constant.ScheduleConstants;
import com.youlai.boot.common.util.BeanUtils;
import com.youlai.boot.common.util.ExceptionUtil;
import com.youlai.boot.common.util.SpringUtils;
import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import com.youlai.boot.deviceJob.model.entity.DeviceJobLog;
import com.youlai.boot.deviceJob.service.DeviceJobLogService;
import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * 抽象quartz调用
 *
 * @author ruoyi
 */
public abstract class AbstractQuartzJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    /**
     * 线程本地变量
     */
    private static ThreadLocal<LocalDateTime> threadLocal = new ThreadLocal<>();

    @Override
    public void execute(JobExecutionContext context) {
        DeviceJob deviceJob = new DeviceJob();
        BeanUtils.copyBeanProp(deviceJob, context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES));
        try {
            before(context, deviceJob);
            doExecute(context, deviceJob);
            after(context, deviceJob, null);
        } catch (Exception e) {
            log.error("任务执行异常  - ：", e);
            after(context, deviceJob, e);
        }
    }

    /**
     * 执行前
     *
     * @param context 工作执行上下文对象
     * @param deviceJob 系统计划任务
     */
    protected void before(JobExecutionContext context, DeviceJob deviceJob) {
        threadLocal.set(LocalDateTime.now());
    }

    /**
     * 执行后
     *
     * @param context 工作执行上下文对象
     * @param deviceJob 系统计划任务
     */
    protected void after(JobExecutionContext context, DeviceJob deviceJob, Exception e) {
        LocalDateTime startTime = threadLocal.get();
        threadLocal.remove();

        final DeviceJobLog jobLog = new DeviceJobLog();
        jobLog.setDeviceId(deviceJob.getDeviceId());
        jobLog.setDeviceName(deviceJob.getDeviceName());
        jobLog.setJobName(deviceJob.getJobName());
        jobLog.setJobGroup(deviceJob.getJobGroup());
        jobLog.setStartTime(startTime);
        jobLog.setStopTime(LocalDateTime.now());
        long runMs = java.time.Duration.between(startTime, jobLog.getStopTime()).toMillis();
        jobLog.setJobMessage(jobLog.getJobName() + " 总共耗时：" + runMs + "毫秒");
        if (e != null) {
            jobLog.setStatus(1);
            String errorMsg = StringUtils.substring(ExceptionUtil.getExceptionMessage(e), 0, 2000);
            jobLog.setExceptionInfo(errorMsg);
        } else {
            jobLog.setStatus(0);
        }

        // 写入数据库当中
        SpringUtils.getBean(DeviceJobLogService.class).save(jobLog);
    }

    /**
     * 执行方法，由子类重载
     *
     * @param context 工作执行上下文对象
     * @param deviceJob 任务信息实体
     * @throws Exception 执行过程中的异常
     */
    protected abstract void doExecute(JobExecutionContext context, DeviceJob deviceJob) throws Exception;
}
