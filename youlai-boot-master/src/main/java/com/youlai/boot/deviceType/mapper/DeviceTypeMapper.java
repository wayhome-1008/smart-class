package com.youlai.boot.deviceType.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.deviceType.model.entity.DeviceType;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.deviceType.model.query.DeviceTypeQuery;
import com.youlai.boot.deviceType.model.vo.DeviceTypeVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备类型字典(自维护)Mapper接口
 *
 * @author way
 * @since 2025-05-19 10:59
 */
@Mapper
public interface DeviceTypeMapper extends BaseMapper<DeviceType> {

    /**
     * 获取设备类型字典(自维护)分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<DeviceTypeVO>} 设备类型字典(自维护)分页列表
     */
    Page<DeviceTypeVO> getDeviceTypePage(Page<DeviceTypeVO> page, DeviceTypeQuery queryParams);

}
