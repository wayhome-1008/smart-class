package com.youlai.boot.room.service;

import com.youlai.boot.common.model.Option;
import com.youlai.boot.floor.model.vo.FloorVO;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.model.form.RoomForm;
import com.youlai.boot.room.model.query.RoomQuery;
import com.youlai.boot.room.model.vo.RoomVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 房间管理服务类
 *
 * @author way
 * @since 2025-05-09 12:09
 */
public interface RoomService extends IService<Room> {

    /**
     *房间管理分页列表
     *
     * @return {@link IPage<RoomVO>} 房间管理分页列表
     */
    IPage<RoomVO> getRoomPage(RoomQuery queryParams);

    /**
     * 获取房间管理表单数据
     *
     * @param id 房间管理ID
     * @return 房间管理表单数据
     */
     RoomForm getRoomFormData(Long id);

    /**
     * 新增房间管理
     *
     * @param formData 房间管理表单对象
     * @return 是否新增成功
     */
    boolean saveRoom(RoomForm formData);

    /**
     * 修改房间管理
     *
     * @param id   房间管理ID
     * @param formData 房间管理表单对象
     * @return 是否修改成功
     */
    boolean updateRoom(Long id, RoomForm formData);

    /**
     * 删除房间管理
     *
     * @param ids 房间管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteRooms(String ids);

    List<Option<Long>> listRoomOptions();

    List<Room> listRoomByFloor(List<FloorVO> records);
}
