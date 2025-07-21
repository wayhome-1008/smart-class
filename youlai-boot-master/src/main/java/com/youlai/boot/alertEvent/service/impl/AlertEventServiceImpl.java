package com.youlai.boot.alertEvent.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.alertEvent.mapper.AlertEventMapper;
import com.youlai.boot.alertEvent.service.AlertEventService;
import com.youlai.boot.alertEvent.model.entity.AlertEvent;
import com.youlai.boot.alertEvent.model.form.AlertEventForm;
import com.youlai.boot.alertEvent.model.query.AlertEventQuery;
import com.youlai.boot.alertEvent.model.vo.AlertEventVO;
import com.youlai.boot.alertEvent.converter.AlertEventConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 报警记录服务实现类
 *
 * @author way
 * @since 2025-07-21 12:16
 */
@Service
@RequiredArgsConstructor
public class AlertEventServiceImpl extends ServiceImpl<AlertEventMapper, AlertEvent> implements AlertEventService {

    private final AlertEventConverter alertEventConverter;

    /**
    * 获取报警记录分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<AlertEventVO>} 报警记录分页列表
    */
    @Override
    public IPage<AlertEventVO> getAlertEventPage(AlertEventQuery queryParams) {
        Page<AlertEventVO> pageVO = this.baseMapper.getAlertEventPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取报警记录表单数据
     *
     * @param id 报警记录ID
     * @return 报警记录表单数据
     */
    @Override
    public AlertEventForm getAlertEventFormData(Long id) {
        AlertEvent entity = this.getById(id);
        return alertEventConverter.toForm(entity);
    }
    
    /**
     * 新增报警记录
     *
     * @param formData 报警记录表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveAlertEvent(AlertEventForm formData) {
        AlertEvent entity = alertEventConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新报警记录
     *
     * @param id   报警记录ID
     * @param formData 报警记录表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateAlertEvent(Long id,AlertEventForm formData) {
        AlertEvent entity = alertEventConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除报警记录
     *
     * @param ids 报警记录ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteAlertEvents(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的报警记录数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
