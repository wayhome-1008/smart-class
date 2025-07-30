package com.youlai.boot.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.scene.model.entity.Action;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.query.ActionQuery;
import com.youlai.boot.scene.model.vo.ActionVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 执行器Mapper接口
 *
 * @author way
 * @since 2025-07-30 17:28
 */
@Mapper
public interface ActionMapper extends BaseMapper<Action> {

    /**
     * 获取执行器分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<ActionVO>} 执行器分页列表
     */
    Page<ActionVO> getActionPage(Page<ActionVO> page, ActionQuery queryParams);

}
