package com.youlai.boot.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.scene.model.entity.Scene;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.query.SceneQuery;
import com.youlai.boot.scene.model.vo.SceneVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 场景交互Mapper接口
 *
 * @author way
 * @since 2025-07-29 11:51
 */
@Mapper
public interface SceneMapper extends BaseMapper<Scene> {

    /**
     * 获取场景交互分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<SceneVO>} 场景交互分页列表
     */
    Page<SceneVO> getScenePage(Page<SceneVO> page, SceneQuery queryParams);

}
