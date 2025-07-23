package com.youlai.boot.alertEvent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.alertEvent.mapper.AlertEventMapper;
import com.youlai.boot.alertEvent.model.entity.AlertEvent;
import com.youlai.boot.alertEvent.model.query.AlertEventQuery;
import com.youlai.boot.alertEvent.model.vo.AlertEventVO;
import com.youlai.boot.alertEvent.service.AlertEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 报警记录服务实现类
 *
 * @author way
 * @since 2025-07-21 12:16
 */
@Service
@RequiredArgsConstructor
public class AlertEventServiceImpl extends ServiceImpl<AlertEventMapper, AlertEvent> implements AlertEventService {

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

    @Override
    public boolean updateStatus(String ids, Integer status) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());
        if (idList.isEmpty()) {
            return false;
        }
        AlertEvent updateEntity = new AlertEvent();
        updateEntity.setStatus(String.valueOf(status));
        return update(updateEntity,
                new LambdaQueryWrapper<AlertEvent>()
                        .in(AlertEvent::getId, idList));
    }
}
