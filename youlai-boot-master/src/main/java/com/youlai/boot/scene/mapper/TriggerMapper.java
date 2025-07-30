package com.youlai.boot.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.scene.model.entity.Trigger;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.query.TriggerQuery;
import com.youlai.boot.scene.model.vo.TriggerVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 触发器Mapper接口
 *
 * @author way
 * @since 2025-07-30 17:25
 */
@Mapper
public interface TriggerMapper extends BaseMapper<Trigger> {

    /**
     * 获取触发器分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<TriggerVO>} 触发器分页列表
     */
    Page<TriggerVO> getTriggerPage(Page<TriggerVO> page, TriggerQuery queryParams);

}
