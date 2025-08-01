package com.youlai.boot.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.entity.SceneDevice;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.query.SceneDeviceQuery;
import com.youlai.boot.scene.model.vo.SceneDeviceVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 场景设备Mapper接口
 *
 * @author way
 * @since 2025-07-29 12:03
 */
@Mapper
public interface SceneDeviceMapper extends BaseMapper<SceneDevice> {

    /**
     * 获取场景设备分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<SceneDeviceVO>} 场景设备分页列表
     */
    Page<SceneDeviceVO> getSceneDevicePage(Page<SceneDeviceVO> page, SceneDeviceQuery queryParams);

}
