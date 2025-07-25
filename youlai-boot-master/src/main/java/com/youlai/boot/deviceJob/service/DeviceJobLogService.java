package com.youlai.boot.deviceJob.service;

import com.youlai.boot.deviceJob.model.entity.DeviceJobLog;
import com.youlai.boot.deviceJob.model.form.DeviceJobLogForm;
import com.youlai.boot.deviceJob.model.query.DeviceJobLogQuery;
import com.youlai.boot.deviceJob.model.vo.DeviceJobLogVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 任务日志服务类
 *
 * @author way
 * @since 2025-07-25 10:58
 */
public interface DeviceJobLogService extends IService<DeviceJobLog> {

    /**
     *任务日志分页列表
     *
     * @return {@link IPage<DeviceJobLogVO>} 任务日志分页列表
     */
    IPage<DeviceJobLogVO> getDeviceJobLogPage(DeviceJobLogQuery queryParams);

    /**
     * 获取任务日志表单数据
     *
     * @param id 任务日志ID
     * @return 任务日志表单数据
     */
     DeviceJobLogForm getDeviceJobLogFormData(Long id);


}
