package com.youlai.boot.deviceJob.service;

import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import com.youlai.boot.deviceJob.model.form.DeviceJobForm;
import com.youlai.boot.deviceJob.model.query.DeviceJobQuery;
import com.youlai.boot.deviceJob.model.vo.DeviceJobVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quartz.SchedulerException;

/**
 * 任务管理服务类
 *
 * @author way
 * @since 2025-06-30 18:27
 */
public interface DeviceJobService extends IService<DeviceJob> {

    /**
     *任务管理分页列表
     *
     * @return {@link IPage<DeviceJobVO>} 任务管理分页列表
     */
    IPage<DeviceJobVO> getDeviceJobPage(DeviceJobQuery queryParams);

    /**
     * 获取任务管理表单数据
     *
     * @param id 任务管理ID
     * @return 任务管理表单数据
     */
     DeviceJobForm getDeviceJobFormData(Long id);

    /**
     * 新增任务管理
     *
     * @param formData 任务管理表单对象
     * @return 是否新增成功
     */
    boolean saveDeviceJob(DeviceJobForm formData) throws SchedulerException;

    /**
     * 修改任务管理
     *
     * @param id   任务管理ID
     * @param formData 任务管理表单对象
     * @return 是否修改成功
     */
    boolean updateDeviceJob(Long id, DeviceJobForm formData) throws SchedulerException;

    /**
     * 删除任务管理
     *
     * @param ids 任务管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteDeviceJobs(String ids) throws SchedulerException;

    boolean pauseJob(Long id) throws SchedulerException;

    boolean resumeJob(Long id) throws SchedulerException;

    boolean runOnce(Long id) throws SchedulerException;
    /**
     * 验证Cron表达式有效性
     */
    boolean checkCronExpression(String cronExpression);
}
