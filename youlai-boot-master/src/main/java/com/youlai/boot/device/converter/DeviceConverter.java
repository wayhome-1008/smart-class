package com.youlai.boot.device.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceForm;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * 设备管理对象转换器
 *
 * @author way
 * @since 2025-05-08 15:16
 */
@Mapper(componentModel = "spring")
public interface DeviceConverter{
    DeviceForm toForm(Device entity);

    Device toEntity(DeviceForm formData);
}