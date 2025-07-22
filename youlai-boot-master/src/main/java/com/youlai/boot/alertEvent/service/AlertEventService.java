package com.youlai.boot.alertEvent.service;

import com.youlai.boot.alertEvent.model.entity.AlertEvent;
import com.youlai.boot.alertEvent.model.form.AlertEventForm;
import com.youlai.boot.alertEvent.model.query.AlertEventQuery;
import com.youlai.boot.alertEvent.model.vo.AlertEventVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 报警记录服务类
 *
 * @author way
 * @since 2025-07-21 12:16
 */
public interface AlertEventService extends IService<AlertEvent> {

    /**
     *报警记录分页列表
     *
     * @return {@link IPage<AlertEventVO>} 报警记录分页列表
     */
    IPage<AlertEventVO> getAlertEventPage(AlertEventQuery queryParams);

}
