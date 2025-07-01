package com.youlai.boot.category.service;

import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.model.form.CategoryForm;
import com.youlai.boot.category.model.query.CategoryQuery;
import com.youlai.boot.category.model.vo.CategoryVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 分类管理服务类
 *
 * @author way
 * @since 2025-07-01 09:17
 */
public interface CategoryService extends IService<Category> {

    /**
     *分类管理分页列表
     *
     * @return {@link IPage<CategoryVO>} 分类管理分页列表
     */
    IPage<CategoryVO> getCategoryPage(CategoryQuery queryParams);

    /**
     * 获取分类管理表单数据
     *
     * @param id 分类管理ID
     * @return 分类管理表单数据
     */
     CategoryForm getCategoryFormData(Long id);

    /**
     * 新增分类管理
     *
     * @param formData 分类管理表单对象
     * @return 是否新增成功
     */
    boolean saveCategory(CategoryForm formData);

    /**
     * 修改分类管理
     *
     * @param id   分类管理ID
     * @param formData 分类管理表单对象
     * @return 是否修改成功
     */
    boolean updateCategory(Long id, CategoryForm formData);

    /**
     * 删除分类管理
     *
     * @param ids 分类管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteCategorys(String ids);

}
