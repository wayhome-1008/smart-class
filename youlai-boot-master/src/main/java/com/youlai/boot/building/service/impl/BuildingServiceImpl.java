package com.youlai.boot.building.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.common.model.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.building.mapper.BuildingMapper;
import com.youlai.boot.building.service.BuildingService;
import com.youlai.boot.building.model.entity.Building;
import com.youlai.boot.building.model.form.BuildingForm;
import com.youlai.boot.building.model.query.BuildingQuery;
import com.youlai.boot.building.model.vo.BuildingVO;
import com.youlai.boot.building.converter.BuildingConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

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

    /**
    * 获取教学楼管理分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<BuildingVO>} 教学楼管理分页列表
    */
    @Override
    public IPage<BuildingVO> getBuildingPage(BuildingQuery queryParams) {
        Page<BuildingVO> pageVO = this.baseMapper.getBuildingPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
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
    public boolean updateBuilding(Long id,BuildingForm formData) {
        Building entity = buildingConverter.toEntity(formData);
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
        return this.removeByIds(idList);
    }

    @Override
    public List<Option<Long>> listBuildingOptions() {
        List<Building> list = this.list(new LambdaQueryWrapper<Building>().eq(Building::getStatus, 1));
        return buildingConverter.toOptions(list);
    }

}
