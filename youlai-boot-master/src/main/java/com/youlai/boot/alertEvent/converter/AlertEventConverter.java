package com.youlai.boot.alertEvent.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.alertEvent.model.entity.AlertEvent;
import com.youlai.boot.alertEvent.model.form.AlertEventForm;

/**
 * 报警记录对象转换器
 *
 * @author way
 * @since 2025-07-21 12:16
 */
@Mapper(componentModel = "spring")
public interface AlertEventConverter{

    AlertEventForm toForm(AlertEvent entity);

    AlertEvent toEntity(AlertEventForm formData);
}