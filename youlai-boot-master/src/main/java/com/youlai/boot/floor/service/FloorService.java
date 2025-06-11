package com.youlai.boot.floor.service;

import com.youlai.boot.common.model.Option;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.floor.model.entity.Floor;
import com.youlai.boot.floor.model.form.FloorForm;
import com.youlai.boot.floor.model.query.FloorQuery;
import com.youlai.boot.floor.model.vo.FloorVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 楼层管理服务类
 *
 * @author way
 * @since 2025-05-08 12:23
 */
public interface FloorService extends IService<Floor> {

    /**
     *楼层管理分页列表
     *
     * @return {@link IPage<FloorVO>} 楼层管理分页列表
     */
    IPage<FloorVO> getFloorPage(FloorQuery queryParams);

    /**
     * 获取楼层管理表单数据
     *
     * @param id 楼层管理ID
     * @return 楼层管理表单数据
     */
     FloorForm getFloorFormData(Long id);

    /**
     * 新增楼层管理
     *
     * @param formData 楼层管理表单对象
     * @return 是否新增成功
     */
    boolean saveFloor(FloorForm formData);

    /**
     * 修改楼层管理
     *
     * @param id   楼层管理ID
     * @param formData 楼层管理表单对象
     * @return 是否修改成功
     */
    boolean updateFloor(Long id, FloorForm formData);

    /**
     * 删除楼层管理
     *
     * @param ids 楼层管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteFloors(String ids);

    List<Option<Long>> listFloorOptionsByBuildingId(Long buildingId);
}
