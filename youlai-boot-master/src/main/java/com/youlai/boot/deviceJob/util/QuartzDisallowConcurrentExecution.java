package com.youlai.boot.deviceJob.util;

import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 定时任务处理（禁止并发执行）
 *
 * @author ruoyi
 *
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, DeviceJob deviceJob) throws Exception {
        JobInvokeUtil.invokeMethod(deviceJob);
    }
}