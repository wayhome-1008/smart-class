package com.youlai.boot.scene.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.scene.mapper.SceneMapper;
import com.youlai.boot.scene.model.entity.Scene;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.scene.mapper.SceneDeviceMapper;
import com.youlai.boot.scene.service.SceneDeviceService;
import com.youlai.boot.scene.model.entity.SceneDevice;
import com.youlai.boot.scene.model.form.SceneDeviceForm;
import com.youlai.boot.scene.model.query.SceneDeviceQuery;
import com.youlai.boot.scene.model.vo.SceneDeviceVO;
import com.youlai.boot.scene.converter.SceneDeviceConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 场景设备服务实现类
 *
 * @author way
 * @since 2025-07-29 12:03
 */
@Service
@RequiredArgsConstructor
public class SceneDeviceServiceImpl extends ServiceImpl<SceneDeviceMapper, SceneDevice> implements SceneDeviceService {
    private final SceneDeviceMapper sceneDeviceMapper;
    private final SceneMapper sceneMapper;
    private final SceneDeviceConverter sceneDeviceConverter;

    /**
     * 获取场景设备分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<SceneDeviceVO>} 场景设备分页列表
     */
    @Override
    public IPage<SceneDeviceVO> getSceneDevicePage(SceneDeviceQuery queryParams) {
        Page<SceneDeviceVO> pageVO = this.baseMapper.getSceneDevicePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }

    /**
     * 获取场景设备表单数据
     *
     * @param id 场景设备ID
     * @return 场景设备表单数据
     */
    @Override
    public SceneDeviceForm getSceneDeviceFormData(Long id) {
        SceneDevice entity = this.getById(id);
        return sceneDeviceConverter.toForm(entity);
    }

    /**
     * 新增场景设备
     *
     * @param formData 场景设备表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveSceneDevice(SceneDeviceForm formData) {
        SceneDevice entity = sceneDeviceConverter.toEntity(formData);
        return this.save(entity);
    }

    /**
     * 更新场景设备
     *
     * @param id   场景设备ID
     * @param formData 场景设备表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateSceneDevice(Long id, SceneDeviceForm formData) {
        SceneDevice entity = sceneDeviceConverter.toEntity(formData);
        return this.updateById(entity);
    }

    /**
     * 删除场景设备
     *
     * @param ids 场景设备ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteSceneDevices(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的场景设备数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    @Override
    public List<Scene> selectTriggerDeviceRelateScenes(SceneDevice sceneDevice) {
        {
            LambdaQueryWrapper<SceneDevice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SceneDevice::getSerialNumber, sceneDevice.getSerialNumber());
            List<SceneDevice> list = this.list(queryWrapper);
            return list.stream()
                    .map(item -> sceneMapper.selectById(item.getSceneId()))
                    .collect(Collectors.toList());
        }
    }

}
