package com.youlai.boot.scene.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.scene.mapper.TriggerMapper;
import com.youlai.boot.scene.service.TriggerService;
import com.youlai.boot.scene.model.entity.Trigger;
import com.youlai.boot.scene.model.form.TriggerForm;
import com.youlai.boot.scene.model.query.TriggerQuery;
import com.youlai.boot.scene.model.vo.TriggerVO;
import com.youlai.boot.scene.converter.TriggerConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 触发器服务实现类
 *
 * @author way
 * @since 2025-07-30 17:25
 */
@Service
@RequiredArgsConstructor
public class TriggerServiceImpl extends ServiceImpl<TriggerMapper, Trigger> implements TriggerService {

    private final TriggerConverter triggerConverter;

    /**
    * 获取触发器分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<TriggerVO>} 触发器分页列表
    */
    @Override
    public IPage<TriggerVO> getTriggerPage(TriggerQuery queryParams) {
        Page<TriggerVO> pageVO = this.baseMapper.getTriggerPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取触发器表单数据
     *
     * @param id 触发器ID
     * @return 触发器表单数据
     */
    @Override
    public TriggerForm getTriggerFormData(Long id) {
        Trigger entity = this.getById(id);
        return triggerConverter.toForm(entity);
    }
    
    /**
     * 新增触发器
     *
     * @param formData 触发器表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveTrigger(TriggerForm formData) {
        Trigger entity = triggerConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新触发器
     *
     * @param id   触发器ID
     * @param formData 触发器表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateTrigger(Long id,TriggerForm formData) {
        Trigger entity = triggerConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除触发器
     *
     * @param ids 触发器ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteTriggers(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的触发器数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
