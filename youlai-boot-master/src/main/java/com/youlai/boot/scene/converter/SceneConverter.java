package com.youlai.boot.scene.converter;

import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.form.SceneForm;
import org.mapstruct.Mapper;

/**
 * 场景交互对象转换器
 *
 * @author way
 * @since 2025-07-29 11:51
 */
@Mapper(componentModel = "spring")
public interface SceneConverter {
    SceneForm toForm(Scene entity);

    Scene toEntity(SceneForm formData);
}