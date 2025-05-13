package com.youlai.boot.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.room.model.entity.Room;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.room.model.query.RoomQuery;
import com.youlai.boot.room.model.vo.RoomVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房间管理Mapper接口
 *
 * @author way
 * @since 2025-05-09 12:09
 */
@Mapper
public interface RoomMapper extends BaseMapper<Room> {

    /**
     * 获取房间管理分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<RoomVO>} 房间管理分页列表
     */
    Page<RoomVO> getRoomPage(Page<RoomVO> page, RoomQuery queryParams);

}
