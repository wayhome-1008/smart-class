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
        return this.baseMapper.getAlertEventPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
    }

}
