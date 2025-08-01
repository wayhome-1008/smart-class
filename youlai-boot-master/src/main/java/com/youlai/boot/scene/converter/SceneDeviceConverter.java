package com.youlai.boot.scene.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.entity.SceneDevice;
import com.youlai.boot.scene.model.form.SceneDeviceForm;

/**
 * 场景设备对象转换器
 *
 * @author way
 * @since 2025-07-29 12:03
 */
@Mapper(componentModel = "spring")
public interface SceneDeviceConverter{

    SceneDeviceForm toForm(SceneDevice entity);

    SceneDevice toEntity(SceneDeviceForm formData);
}