package com.youlai.boot.room.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.common.model.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.room.mapper.RoomMapper;
import com.youlai.boot.room.service.RoomService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.model.form.RoomForm;
import com.youlai.boot.room.model.query.RoomQuery;
import com.youlai.boot.room.model.vo.RoomVO;
import com.youlai.boot.room.converter.RoomConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

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
        Page<RoomVO> pageVO = this.baseMapper.getRoomPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
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

}
