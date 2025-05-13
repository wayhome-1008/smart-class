package com.youlai.boot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.device.model.entity.Device;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.device.model.query.DeviceQuery;
import com.youlai.boot.device.model.vo.DeviceVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备管理Mapper接口
 *
 * @author way
 * @since 2025-05-08 15:16
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    /**
     * 获取设备管理分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<DeviceVO>} 设备管理分页列表
     */
    Page<DeviceVO> getDevicePage(Page<DeviceVO> page, DeviceQuery queryParams);

}
