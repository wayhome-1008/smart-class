package com.youlai.boot.deviceType.converter;

import com.youlai.boot.common.model.Option;
import com.youlai.boot.room.model.entity.Room;
import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.deviceType.model.entity.DeviceType;
import com.youlai.boot.deviceType.model.form.DeviceTypeForm;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 设备类型字典(自维护)对象转换器
 *
 * @author way
 * @since 2025-05-19 10:59
 */
@Mapper(componentModel = "spring")
public interface DeviceTypeConverter{

    DeviceTypeForm toForm(DeviceType entity);

    DeviceType toEntity(DeviceTypeForm formData);
    @Mappings({
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "label", source = "deviceType")
    })
    Option<Long> toOption(DeviceType entity);
    List<Option<Long>> toOptions(List<DeviceType> deviceTypeList);
}