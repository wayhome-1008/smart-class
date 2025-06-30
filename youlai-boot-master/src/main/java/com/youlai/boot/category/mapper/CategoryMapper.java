package com.youlai.boot.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.category.model.entity.Category;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.category.model.query.CategoryQuery;
import com.youlai.boot.category.model.vo.CategoryVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类管理Mapper接口
 *
 * @author way
 * @since 2025-06-30 18:52
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 获取分类管理分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<CategoryVO>} 分类管理分页列表
     */
    Page<CategoryVO> getCategoryPage(Page<CategoryVO> page, CategoryQuery queryParams);

}
