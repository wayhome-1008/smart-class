package com.youlai.boot.building.service;

import com.youlai.boot.building.model.entity.Building;
import com.youlai.boot.building.model.form.BuildingForm;
import com.youlai.boot.building.model.query.BuildingQuery;
import com.youlai.boot.building.model.vo.BuildingVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.common.model.Option;

import java.util.List;

/**
 * 教学楼管理服务类
 *
 * @author way
 * @since 2025-05-08 14:00
 */
public interface BuildingService extends IService<Building> {

    /**
     *教学楼管理分页列表
     *
     * @return {@link IPage<BuildingVO>} 教学楼管理分页列表
     */
    IPage<BuildingVO> getBuildingPage(BuildingQuery queryParams);

    /**
     * 获取教学楼管理表单数据
     *
     * @param id 教学楼管理ID
     * @return 教学楼管理表单数据
     */
     BuildingForm getBuildingFormData(Long id);

    /**
     * 新增教学楼管理
     *
     * @param formData 教学楼管理表单对象
     * @return 是否新增成功
     */
    boolean saveBuilding(BuildingForm formData);

    /**
     * 修改教学楼管理
     *
     * @param id   教学楼管理ID
     * @param formData 教学楼管理表单对象
     * @return 是否修改成功
     */
    boolean updateBuilding(Long id, BuildingForm formData);

    /**
     * 删除教学楼管理
     *
     * @param ids 教学楼管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteBuildings(String ids);

    /**
     * 角色下拉列表
     *
     */
    List<Option<Long>> listBuildingOptions();

    List<Option<String>> buildingStructureOptions();
}
