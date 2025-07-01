package com.youlai.boot.deviceJob.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *@Author: way
 *@CreateTime: 2025-07-01  10:04
 *@Description: TODO
 */
@Slf4j
public class DeviceSyncJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String deviceId = jobExecutionContext.getJobDetail().getJobDataMap().getString("deviceId");
        log.info("Syncing device: {}", deviceId);
    }
}
