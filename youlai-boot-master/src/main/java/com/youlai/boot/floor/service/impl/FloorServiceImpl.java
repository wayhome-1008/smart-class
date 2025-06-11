package com.youlai.boot.floor.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.floor.converter.FloorConverter;
import com.youlai.boot.floor.mapper.FloorMapper;
import com.youlai.boot.floor.model.entity.Floor;
import com.youlai.boot.floor.model.form.FloorForm;
import com.youlai.boot.floor.model.query.FloorQuery;
import com.youlai.boot.floor.model.vo.FloorVO;
import com.youlai.boot.floor.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 楼层管理服务实现类
 *
 * @author way
 * @since 2025-05-08 12:23
 */
@Service
@RequiredArgsConstructor
public class FloorServiceImpl extends ServiceImpl<FloorMapper, Floor> implements FloorService {
    private final FloorConverter floorConverter;
    /**
    * 获取楼层管理分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<FloorVO>} 楼层管理分页列表
    */
    @Override
    public IPage<FloorVO> getFloorPage(FloorQuery queryParams) {
        Page<FloorVO> pageVO = this.baseMapper.getFloorPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取楼层管理表单数据
     *
     * @param id 楼层管理ID
     * @return 楼层管理表单数据
     */
    @Override
    public FloorForm getFloorFormData(Long id) {
        Floor entity = this.getById(id);
        return floorConverter.toForm(entity);
    }
    
    /**
     * 新增楼层管理
     *
     * @param formData 楼层管理表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveFloor(FloorForm formData) {
        Floor entity = floorConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新楼层管理
     *
     * @param id   楼层管理ID
     * @param formData 楼层管理表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateFloor(Long id,FloorForm formData) {
        Floor entity = floorConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除楼层管理
     *
     * @param ids 楼层管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteFloors(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的楼层管理数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    @Override
    public List<Option<Long>> listFloorOptionsByBuildingId(Long buildingId) {
        List<Floor> list = this.list(new LambdaQueryWrapper<Floor>().eq(Floor::getBuildingId, buildingId));
            return floorConverter.toOptions(list);
    }

}
