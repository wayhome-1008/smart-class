package com.youlai.boot.deviceJob.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.deviceJob.converter.DeviceJobLogConverter;
import com.youlai.boot.deviceJob.mapper.DeviceJobLogMapper;
import com.youlai.boot.deviceJob.model.entity.DeviceJobLog;
import com.youlai.boot.deviceJob.model.form.DeviceJobLogForm;
import com.youlai.boot.deviceJob.model.query.DeviceJobLogQuery;
import com.youlai.boot.deviceJob.model.vo.DeviceJobLogVO;
import com.youlai.boot.deviceJob.service.DeviceJobLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 任务日志服务实现类
 *
 * @author way
 * @since 2025-07-25 10:58
 */
@Service
@RequiredArgsConstructor
public class DeviceJobLogServiceImpl extends ServiceImpl<DeviceJobLogMapper, DeviceJobLog> implements DeviceJobLogService {

    private final DeviceJobLogConverter deviceJobLogConverter;

    /**
    * 获取任务日志分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<DeviceJobLogVO>} 任务日志分页列表
    */
    @Override
    public IPage<DeviceJobLogVO> getDeviceJobLogPage(DeviceJobLogQuery queryParams) {
        return this.baseMapper.getDeviceJobLogPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
    }
    
    /**
     * 获取任务日志表单数据
     *
     * @param id 任务日志ID
     * @return 任务日志表单数据
     */
    @Override
    public DeviceJobLogForm getDeviceJobLogFormData(Long id) {
        DeviceJobLog entity = this.getById(id);
        return deviceJobLogConverter.toForm(entity);
    }


}
