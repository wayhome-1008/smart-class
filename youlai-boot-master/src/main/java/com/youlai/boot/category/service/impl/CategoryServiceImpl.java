package com.youlai.boot.category.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.category.mapper.CategoryMapper;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.model.form.CategoryForm;
import com.youlai.boot.category.model.query.CategoryQuery;
import com.youlai.boot.category.model.vo.CategoryVO;
import com.youlai.boot.category.converter.CategoryConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 分类管理服务实现类
 *
 * @author way
 * @since 2025-07-01 09:17
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final CategoryConverter categoryConverter;

    /**
    * 获取分类管理分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<CategoryVO>} 分类管理分页列表
    */
    @Override
    public IPage<CategoryVO> getCategoryPage(CategoryQuery queryParams) {
        Page<CategoryVO> pageVO = this.baseMapper.getCategoryPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取分类管理表单数据
     *
     * @param id 分类管理ID
     * @return 分类管理表单数据
     */
    @Override
    public CategoryForm getCategoryFormData(Long id) {
        Category entity = this.getById(id);
        return categoryConverter.toForm(entity);
    }
    
    /**
     * 新增分类管理
     *
     * @param formData 分类管理表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveCategory(CategoryForm formData) {
        Category entity = categoryConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新分类管理
     *
     * @param id   分类管理ID
     * @param formData 分类管理表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateCategory(Long id,CategoryForm formData) {
        Category entity = categoryConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除分类管理
     *
     * @param ids 分类管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteCategorys(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的分类管理数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
