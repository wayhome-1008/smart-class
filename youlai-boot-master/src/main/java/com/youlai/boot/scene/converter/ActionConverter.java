package com.youlai.boot.scene.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.entity.Action;
import com.youlai.boot.scene.model.form.ActionForm;

/**
 * 执行器对象转换器
 *
 * @author way
 * @since 2025-07-30 17:28
 */
@Mapper(componentModel = "spring")
public interface ActionConverter{

    ActionForm toForm(Action entity);

    Action toEntity(ActionForm formData);
}