package com.youlai.boot.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.scene.model.entity.SceneScript;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.query.SceneScriptQuery;
import com.youlai.boot.scene.model.vo.SceneScriptVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 场景脚本Mapper接口
 *
 * @author way
 * @since 2025-07-29 11:58
 */
@Mapper
public interface SceneScriptMapper extends BaseMapper<SceneScript> {

    /**
     * 获取场景脚本分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<SceneScriptVO>} 场景脚本分页列表
     */
    Page<SceneScriptVO> getSceneScriptPage(Page<SceneScriptVO> page, SceneScriptQuery queryParams);

}
