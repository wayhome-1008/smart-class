package com.youlai.boot.deviceJob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.deviceJob.model.query.DeviceJobQuery;
import com.youlai.boot.deviceJob.model.vo.DeviceJobVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务管理Mapper接口
 *
 * @author way
 * @since 2025-06-30 18:27
 */
@Mapper
public interface DeviceJobMapper extends BaseMapper<DeviceJob> {

    /**
     * 获取任务管理分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<DeviceJobVO>} 任务管理分页列表
     */
    Page<DeviceJobVO> getDeviceJobPage(Page<DeviceJobVO> page, DeviceJobQuery queryParams);

}
