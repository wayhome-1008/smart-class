package com.youlai.boot.scene.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.form.SceneForm;

/**
 * 场景交互对象转换器
 *
 * @author way
 * @since 2025-07-29 11:51
 */
@Mapper(componentModel = "spring")
public interface SceneConverter{

    SceneForm toForm(Scene entity);

    Scene toEntity(SceneForm formData);
}