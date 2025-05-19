package com.youlai.boot.deviceType.service.impl;

import com.youlai.boot.common.model.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.deviceType.mapper.DeviceTypeMapper;
import com.youlai.boot.deviceType.service.DeviceTypeService;
import com.youlai.boot.deviceType.model.entity.DeviceType;
import com.youlai.boot.deviceType.model.form.DeviceTypeForm;
import com.youlai.boot.deviceType.model.query.DeviceTypeQuery;
import com.youlai.boot.deviceType.model.vo.DeviceTypeVO;
import com.youlai.boot.deviceType.converter.DeviceTypeConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 设备类型字典(自维护)服务实现类
 *
 * @author way
 * @since 2025-05-19 10:59
 */
@Service
@RequiredArgsConstructor
public class DeviceTypeServiceImpl extends ServiceImpl<DeviceTypeMapper, DeviceType> implements DeviceTypeService {

    private final DeviceTypeConverter deviceTypeConverter;

    /**
    * 获取设备类型字典(自维护)分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<DeviceTypeVO>} 设备类型字典(自维护)分页列表
    */
    @Override
    public IPage<DeviceTypeVO> getDeviceTypePage(DeviceTypeQuery queryParams) {
        Page<DeviceTypeVO> pageVO = this.baseMapper.getDeviceTypePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取设备类型字典(自维护)表单数据
     *
     * @param id 设备类型字典(自维护)ID
     * @return 设备类型字典(自维护)表单数据
     */
    @Override
    public DeviceTypeForm getDeviceTypeFormData(Long id) {
        DeviceType entity = this.getById(id);
        return deviceTypeConverter.toForm(entity);
    }
    
    /**
     * 新增设备类型字典(自维护)
     *
     * @param formData 设备类型字典(自维护)表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveDeviceType(DeviceTypeForm formData) {
        DeviceType entity = deviceTypeConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新设备类型字典(自维护)
     *
     * @param id   设备类型字典(自维护)ID
     * @param formData 设备类型字典(自维护)表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateDeviceType(Long id,DeviceTypeForm formData) {
        DeviceType entity = deviceTypeConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除设备类型字典(自维护)
     *
     * @param ids 设备类型字典(自维护)ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteDeviceTypes(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的设备类型字典(自维护)数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    @Override
    public List<Option<Long>> listDeviceTypeOptions() {
       List<DeviceType>  deviceTypeList=this.list();
       return deviceTypeConverter.toOptions(deviceTypeList);
    }

}
