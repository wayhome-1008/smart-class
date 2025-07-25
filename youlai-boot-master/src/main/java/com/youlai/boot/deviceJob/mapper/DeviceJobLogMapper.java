package com.youlai.boot.deviceJob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.deviceJob.model.entity.DeviceJobLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.deviceJob.model.query.DeviceJobLogQuery;
import com.youlai.boot.deviceJob.model.vo.DeviceJobLogVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务日志Mapper接口
 *
 * @author way
 * @since 2025-07-25 10:58
 */
@Mapper
public interface DeviceJobLogMapper extends BaseMapper<DeviceJobLog> {

    /**
     * 获取任务日志分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<DeviceJobLogVO>} 任务日志分页列表
     */
    Page<DeviceJobLogVO> getDeviceJobLogPage(Page<DeviceJobLogVO> page, DeviceJobLogQuery queryParams);

}
