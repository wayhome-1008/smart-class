package com.youlai.boot.building.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.building.model.entity.Building;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.building.model.query.BuildingQuery;
import com.youlai.boot.building.model.vo.BuildingVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学楼管理Mapper接口
 *
 * @author way
 * @since 2025-05-08 14:00
 */
@Mapper
public interface BuildingMapper extends BaseMapper<Building> {

    /**
     * 获取教学楼管理分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<BuildingVO>} 教学楼管理分页列表
     */
    Page<BuildingVO> getBuildingPage(Page<BuildingVO> page, BuildingQuery queryParams);

}
