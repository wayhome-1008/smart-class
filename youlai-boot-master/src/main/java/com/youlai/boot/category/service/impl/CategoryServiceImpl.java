package com.youlai.boot.category.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.category.converter.CategoryConverter;
import com.youlai.boot.category.mapper.CategoryMapper;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.model.form.BindingForm;
import com.youlai.boot.category.model.form.CategoryForm;
import com.youlai.boot.category.model.query.CategoryQuery;
import com.youlai.boot.category.model.vo.CategoryVO;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;
import com.youlai.boot.categoryDeviceRelationship.service.CategoryDeviceRelationshipService;
import com.youlai.boot.common.model.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private final CategoryDeviceRelationshipService categoryDeviceRelationshipService;

    /**
     * 获取分类管理分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<CategoryVO>} 分类管理分页列表
     */
    @Override
    public IPage<CategoryVO> getCategoryPage(CategoryQuery queryParams) {
        return this.baseMapper.getCategoryPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
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
        //查询是否分类名称重复
        long count = this.count(new LambdaQueryWrapper<Category>()
                .eq(Category::getCategoryName, entity.getCategoryName()));
        Assert.isTrue(count == 0, "分类名称已存在");
//        //存储分类设备关系
//        if (StrUtil.isNotBlank(formData.getDeviceIds())) {
//            List<Long> deviceIds = Arrays.stream(formData.getDeviceIds().split(","))
//                    .map(Long::parseLong)
//                    .toList();
//            List<CategoryDeviceRelationship> relationships = new ArrayList<>();
//            deviceIds.forEach(deviceId -> {
//                CategoryDeviceRelationship relationship = new CategoryDeviceRelationship();
//                relationship.setCategoryId(entity.getId());
//                relationship.setDeviceId(deviceId);
//                relationships.add(relationship);
//            });
//            categoryDeviceRelationshipService.saveBatch(relationships);
//        }
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
    public boolean updateCategory(Long id, CategoryForm formData) {
        Category entity = categoryConverter.toEntity(formData);
        //查询是否分类名称重复
        long count = this.count(new LambdaQueryWrapper<Category>().ne(Category::getId, id)
                .eq(Category::getCategoryName, entity.getCategoryName()));
        Assert.isTrue(count == 0, "分类名称已存在");
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

    @Override
    public boolean bindCategory(BindingForm formData) {
        // 1. 参数校验
        Assert.notNull(formData.getCategoryId(), "分类ID不能为空");
        Assert.isTrue(StrUtil.isNotBlank(formData.getDeviceIds()), "设备ID列表不能为空");

        // 2. 解析设备ID列表
        List<Long> deviceIds = Arrays.stream(formData.getDeviceIds().split(","))
                .map(Long::parseLong)
                .toList();

        // 3. 查询已存在的绑定关系
        List<CategoryDeviceRelationship> existingBindings = categoryDeviceRelationshipService.list(
                new LambdaQueryWrapper<CategoryDeviceRelationship>()
                        .eq(CategoryDeviceRelationship::getCategoryId, formData.getCategoryId())
                        .in(CategoryDeviceRelationship::getDeviceId, deviceIds)
        );

        // 4. 过滤出需要新增的设备ID（排除已存在的）
        List<Long> existingDeviceIds = existingBindings.stream()
                .map(CategoryDeviceRelationship::getDeviceId)
                .toList();

        List<CategoryDeviceRelationship> newBindings = deviceIds.stream()
                .filter(deviceId -> !existingDeviceIds.contains(deviceId))
                .map(deviceId -> {
                    CategoryDeviceRelationship relationship = new CategoryDeviceRelationship();
                    relationship.setCategoryId(formData.getCategoryId());
                    relationship.setDeviceId(deviceId);
                    return relationship;
                })
                .toList();

        // 5. 批量插入新绑定关系（如果有需要新增的）
        if (!newBindings.isEmpty()) {
            return categoryDeviceRelationshipService.saveBatch(newBindings);
        }

        return true; // 全部已存在时也返回成功
    }

    @Override
    public List<Option<Long>> listCategoryOptions() {
        List<Category> list = this.list(new LambdaQueryWrapper<Category>().eq(Category::getStatus, 1));
        return categoryConverter.toOptions(list);
    }

}
