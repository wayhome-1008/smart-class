package com.youlai.boot.system.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.system.model.entity.AlertRule;
import com.youlai.boot.system.model.form.AlertRuleForm;

/**
 * 报警配置对象转换器
 *
 * @author way
 * @since 2025-07-21 11:22
 */
@Mapper(componentModel = "spring")
public interface AlertRuleConverter{

    AlertRuleForm toForm(AlertRule entity);

    AlertRule toEntity(AlertRuleForm formData);
}