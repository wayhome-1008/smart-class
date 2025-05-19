package com.youlai.boot.deviceType.service;

import com.youlai.boot.common.model.Option;
import com.youlai.boot.deviceType.model.entity.DeviceType;
import com.youlai.boot.deviceType.model.form.DeviceTypeForm;
import com.youlai.boot.deviceType.model.query.DeviceTypeQuery;
import com.youlai.boot.deviceType.model.vo.DeviceTypeVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 设备类型字典(自维护)服务类
 *
 * @author way
 * @since 2025-05-19 10:59
 */
public interface DeviceTypeService extends IService<DeviceType> {

    /**
     *设备类型字典(自维护)分页列表
     *
     * @return {@link IPage<DeviceTypeVO>} 设备类型字典(自维护)分页列表
     */
    IPage<DeviceTypeVO> getDeviceTypePage(DeviceTypeQuery queryParams);

    /**
     * 获取设备类型字典(自维护)表单数据
     *
     * @param id 设备类型字典(自维护)ID
     * @return 设备类型字典(自维护)表单数据
     */
     DeviceTypeForm getDeviceTypeFormData(Long id);

    /**
     * 新增设备类型字典(自维护)
     *
     * @param formData 设备类型字典(自维护)表单对象
     * @return 是否新增成功
     */
    boolean saveDeviceType(DeviceTypeForm formData);

    /**
     * 修改设备类型字典(自维护)
     *
     * @param id   设备类型字典(自维护)ID
     * @param formData 设备类型字典(自维护)表单对象
     * @return 是否修改成功
     */
    boolean updateDeviceType(Long id, DeviceTypeForm formData);

    /**
     * 删除设备类型字典(自维护)
     *
     * @param ids 设备类型字典(自维护)ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteDeviceTypes(String ids);

    List<Option<Long>> listDeviceTypeOptions();
}
