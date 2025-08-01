package com.youlai.boot.scene.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.entity.SceneScript;
import com.youlai.boot.scene.model.form.SceneScriptForm;

/**
 * 场景脚本对象转换器
 *
 * @author way
 * @since 2025-07-29 11:58
 */
@Mapper(componentModel = "spring")
public interface SceneScriptConverter{

    SceneScriptForm toForm(SceneScript entity);

    SceneScript toEntity(SceneScriptForm formData);
}