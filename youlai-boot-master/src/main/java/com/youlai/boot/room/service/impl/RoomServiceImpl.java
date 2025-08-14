package com.youlai.boot.room.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.core.security.util.SecurityUtils;
import com.youlai.boot.floor.model.vo.FloorVO;
import com.youlai.boot.room.converter.RoomConverter;
import com.youlai.boot.room.mapper.RoomMapper;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.model.form.RoomForm;
import com.youlai.boot.room.model.query.RoomQuery;
import com.youlai.boot.room.model.vo.RoomVO;
import com.youlai.boot.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 房间管理服务实现类
 *
 * @author way
 * @since 2025-05-09 12:09
 */
@Service
@RequiredArgsConstructor
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {

    private final RoomConverter roomConverter;

    /**
     * 获取房间管理分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<RoomVO>} 房间管理分页列表
     */
    @Override
    public IPage<RoomVO> getRoomPage(RoomQuery queryParams) {
        return this.baseMapper.getRoomPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
    }

    /**
     * 获取房间管理表单数据
     *
     * @param id 房间管理ID
     * @return 房间管理表单数据
     */
    @Override
    public RoomForm getRoomFormData(Long id) {
        Room entity = this.getById(id);
        return roomConverter.toForm(entity);
    }

    /**
     * 新增房间管理
     *
     * @param formData 房间管理表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveRoom(RoomForm formData) {
        Room entity = roomConverter.toEntity(formData);
//        //如果有部门需校验是否该部门已被使用
//        if (entity.getDepartmentId() != null) {
//            long count = this.count(new LambdaQueryWrapper<Room>().eq(Room::getDepartmentId, entity.getDepartmentId()));
//            Assert.isTrue(count == 0, "部门已使用");
//        }
        entity.setCreateBy(SecurityUtils.getUserId());
        return this.save(entity);
    }

    /**
     * 更新房间管理
     *
     * @param id   房间管理ID
     * @param formData 房间管理表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateRoom(Long id, RoomForm formData) {
        Room entity = roomConverter.toEntity(formData);
        // 如果部门ID不为空且发生了变化，才需要校验
//        if (entity.getDepartmentId() != null) {
//            // 获取当前房间的原始信息
//            Room oldRoom = this.getById(id);
//            // 只有当部门ID真正改变时才进行校验
//            if (oldRoom != null && !entity.getDepartmentId().equals(oldRoom.getDepartmentId())) {
//                // 校验新部门是否已被其他房间使用（排除当前房间）
//                long count = this.count(new LambdaQueryWrapper<Room>()
//                        .eq(Room::getDepartmentId, entity.getDepartmentId())
//                        .ne(Room::getId, id));
//                Assert.isTrue(count == 0, "部门已使用");
//            }
//        }
        entity.setUpdateBy(SecurityUtils.getUserId());
        return this.updateById(entity);
    }

    /**
     * 删除房间管理
     *
     * @param ids 房间管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteRooms(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的房间管理数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    @Override
    public List<Option<Long>> listRoomOptions() {
        List<Room> list = this.list();
        return roomConverter.toOptions(list);
    }

    @Override
    public List<Room> listRoomByFloor(List<FloorVO> records) {
        return this.list(new LambdaQueryWrapper<Room>().in(Room::getFloorId, records.stream().map(FloorVO::getId).toArray()));
    }

    @Override
    public boolean deleteByBuildingIds(List<Long> buildingIds) {
        return this.remove(new LambdaQueryWrapper<Room>().in(Room::getBuildingId, buildingIds));
    }

    @Override
    public boolean deleteByFloorIds(List<Long> floorIds) {
        return this.remove(new LambdaQueryWrapper<Room>().in(Room::getFloorId, floorIds));
    }

}
