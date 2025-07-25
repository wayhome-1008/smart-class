package com.youlai.boot.deviceJob.converter;

import com.youlai.boot.deviceJob.model.entity.DeviceJobLog;
import com.youlai.boot.deviceJob.model.form.DeviceJobLogForm;
import org.mapstruct.Mapper;

/**
 * 任务日志对象转换器
 *
 * @author way
 * @since 2025-07-25 10:58
 */
@Mapper(componentModel = "spring")
public interface DeviceJobLogConverter{

    DeviceJobLogForm toForm(DeviceJobLog entity);

    DeviceJobLog toEntity(DeviceJobLogForm formData);
}