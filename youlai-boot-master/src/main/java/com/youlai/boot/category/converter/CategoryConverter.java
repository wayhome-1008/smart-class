package com.youlai.boot.category.converter;

import com.youlai.boot.common.model.Option;
import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.model.form.CategoryForm;

import java.util.List;

/**
 * 分类管理对象转换器
 *
 * @author way
 * @since 2025-07-01 09:17
 */
@Mapper(componentModel = "spring")
public interface CategoryConverter{

    CategoryForm toForm(Category entity);

    Category toEntity(CategoryForm formData);

    List<Option<Long>> toOptions(List<Category> list);
}