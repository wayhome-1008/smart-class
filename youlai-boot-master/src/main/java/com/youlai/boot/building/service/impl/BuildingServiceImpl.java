package com.youlai.boot.building.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.building.converter.BuildingConverter;
import com.youlai.boot.building.mapper.BuildingMapper;
import com.youlai.boot.building.model.entity.Building;
import com.youlai.boot.building.model.form.BuildingForm;
import com.youlai.boot.building.model.query.BuildingQuery;
import com.youlai.boot.building.model.vo.BuildingVO;
import com.youlai.boot.building.service.BuildingService;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.core.security.util.SecurityUtils;
import com.youlai.boot.floor.model.entity.Floor;
import com.youlai.boot.floor.service.FloorService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 教学楼管理服务实现类
 *
 * @author way
 * @since 2025-05-08 14:00
 */
@Service
@RequiredArgsConstructor
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements BuildingService {

    private final BuildingConverter buildingConverter;
    private final RoomService roomService;
    private final FloorService floorService;

    /**
     * 获取教学楼管理分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<BuildingVO>} 教学楼管理分页列表
     */
    @Override
    public IPage<BuildingVO> getBuildingPage(BuildingQuery queryParams) {
        return this.baseMapper.getBuildingPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
    }

    /**
     * 获取教学楼管理表单数据
     *
     * @param id 教学楼管理ID
     * @return 教学楼管理表单数据
     */
    @Override
    public BuildingForm getBuildingFormData(Long id) {
        Building entity = this.getById(id);
        return buildingConverter.toForm(entity);
    }

    /**
     * 新增教学楼管理
     *
     * @param formData 教学楼管理表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveBuilding(BuildingForm formData) {
        Building entity = buildingConverter.toEntity(formData);
        entity.setCreateBy(SecurityUtils.getUserId());
        return this.save(entity);
    }

    /**
     * 更新教学楼管理
     *
     * @param id   教学楼管理ID
     * @param formData 教学楼管理表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateBuilding(Long id, BuildingForm formData) {
        Building entity = buildingConverter.toEntity(formData);
        entity.setUpdateBy(SecurityUtils.getUserId());
        return this.updateById(entity);
    }

    /**
     * 删除教学楼管理
     *
     * @param ids 教学楼管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteBuildings(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的教学楼管理数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        roomService.deleteByBuildingIds(idList);
        floorService.deleteByBuildingIds(idList);
        //先删房间 再删楼层   最后删教学楼
        return this.removeByIds(idList);
    }

    @Override
    public List<Option<Long>> listBuildingOptions() {
        List<Building> list = this.list(new LambdaQueryWrapper<Building>().eq(Building::getStatus, 1));
        return buildingConverter.toOptions(list);
    }

    @Override
    public List<Option<String>> buildingStructureOptions() {
        // 1. 查询所有楼宇
        List<Building> buildings = baseMapper.selectList(null);

        // 2. 转换为Option列表并添加楼层子节点，使用复合ID解决ID重复问题
        return buildings.stream().map(building -> {
            // 2.1 查询当前楼宇的所有楼层
            List<Floor> floors = floorService.list(
                    new LambdaQueryWrapper<Floor>()
                            .eq(Floor::getBuildingId, building.getId())
            );

            // 2.2 转换楼层为Option并添加房间子节点
            List<Option<String>> floorOptions = floors.stream().map(floor -> {
                // 查询当前楼层的所有房间
                List<Room> classrooms = roomService.list(
                        new LambdaQueryWrapper<Room>()
                                .eq(Room::getFloorId, floor.getId())
                                .eq(Room::getBuildingId, building.getId())
                );

                // 转换房间为Option，使用"room_"前缀
                List<Option<String>> classroomOptions = classrooms.stream()
                        .map(classroom -> new Option<>(
                                "room_" + classroom.getId(),
                                classroom.getClassroomCode()
                        ))
                        .collect(Collectors.toList());

                // 创建楼层Option，使用"floor_"前缀，包含房间子节点
                return new Option<>(
                        "floor_" + floor.getId(),
                        floor.getFloorNumber(),
                        classroomOptions
                );
            }).collect(Collectors.toList());

            // 创建楼宇Option，使用"building_"前缀，包含楼层子节点
            return new Option<>(
                    "building_" + building.getId(),
                    building.getBuildingName(),
                    floorOptions
            );
        }).collect(Collectors.toList());
    }

}
