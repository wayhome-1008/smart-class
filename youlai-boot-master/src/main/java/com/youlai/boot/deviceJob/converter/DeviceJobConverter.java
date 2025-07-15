package com.youlai.boot.deviceJob.converter;

import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import com.youlai.boot.deviceJob.model.form.DeviceJobForm;
import org.mapstruct.Mapper;

/**
 * 任务管理对象转换器
 *
 * @author way
 * @since 2025-06-30 18:27
 */
@Mapper(componentModel = "spring")
public interface DeviceJobConverter{

    DeviceJobForm toForm(DeviceJob entity);

    DeviceJob toEntity(DeviceJobForm formData);
}