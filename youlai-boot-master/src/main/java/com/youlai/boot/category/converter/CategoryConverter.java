package com.youlai.boot.category.converter;

import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.model.form.CategoryForm;

/**
 * 分类管理对象转换器
 *
 * @author way
 * @since 2025-06-30 18:52
 */
@Mapper(componentModel = "spring")
public interface CategoryConverter{

    CategoryForm toForm(Category entity);

    Category toEntity(CategoryForm formData);
}