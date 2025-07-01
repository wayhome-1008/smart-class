package com.youlai.boot.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
    private final DeviceService deviceService;

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
        boolean success;
        Category entity = categoryConverter.toEntity(formData);
        success = this.save(entity);
        //看是否需要绑定设备
        if (StringUtils.isNotBlank(formData.getDeviceIds())) {
            //在设备上更新上分类id
            List<Long> idList = Arrays.stream(formData.getDeviceIds().split(","))
                    .map(Long::parseLong)
                    .toList();
            LambdaUpdateWrapper<Device> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.in(Device::getId, idList)  // 指定ID列表
                    .set(Device::getCategoryId, entity.getId()); // 设置要更新的字段和值
            success = deviceService.update(updateWrapper);
        }
        return success;
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
        boolean success;
        Category entity = categoryConverter.toEntity(formData);
        success = this.updateById(entity);
        //看是否需要绑定设备
        if (StringUtils.isNotBlank(formData.getDeviceIds())) {
            //在设备上更新上分类id
            List<Long> idList = Arrays.stream(formData.getDeviceIds().split(","))
                    .map(Long::parseLong)
                    .toList();
            LambdaUpdateWrapper<Device> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.in(Device::getId, idList)  // 指定ID列表
                    .set(Device::getCategoryId, entity.getId()); // 设置要更新的字段和值
            success = deviceService.update(updateWrapper);
        }
        return success;
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
        List<Device> deviceList = deviceService.list(new LambdaQueryWrapper<Device>().in(Device::getCategoryId, idList));
        if (!deviceList.isEmpty()) {
            // 删除的分类管理下有设备，则不允许删除
            throw new RuntimeException("请先删除该分类下的设备");
        }
        return this.removeByIds(idList);
    }

}
