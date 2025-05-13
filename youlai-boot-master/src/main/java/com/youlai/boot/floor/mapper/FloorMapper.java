package com.youlai.boot.floor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.floor.model.entity.Floor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.floor.model.query.FloorQuery;
import com.youlai.boot.floor.model.vo.FloorVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 楼层管理Mapper接口
 *
 * @author way
 * @since 2025-05-08 12:23
 */
@Mapper
public interface FloorMapper extends BaseMapper<Floor> {

    /**
     * 获取楼层管理分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<FloorVO>} 楼层管理分页列表
     */
    Page<FloorVO> getFloorPage(Page<FloorVO> page, FloorQuery queryParams);

}
