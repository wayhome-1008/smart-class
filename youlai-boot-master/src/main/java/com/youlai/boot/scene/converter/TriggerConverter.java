package com.youlai.boot.scene.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.entity.Trigger;
import com.youlai.boot.scene.model.form.TriggerForm;

/**
 * 触发器对象转换器
 *
 * @author way
 * @since 2025-07-30 17:25
 */
@Mapper(componentModel = "spring")
public interface TriggerConverter{

    TriggerForm toForm(Trigger entity);

    Trigger toEntity(TriggerForm formData);
}