package com.youlai.boot.scene.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.entity.RuleScript;
import com.youlai.boot.scene.model.form.RuleScriptForm;

/**
 * 规则引擎脚本对象转换器
 *
 * @author way
 * @since 2025-07-29 11:54
 */
@Mapper(componentModel = "spring")
public interface RuleScriptConverter{

    RuleScriptForm toForm(RuleScript entity);

    RuleScript toEntity(RuleScriptForm formData);
}