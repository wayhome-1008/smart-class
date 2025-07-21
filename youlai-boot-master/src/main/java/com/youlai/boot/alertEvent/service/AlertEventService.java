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

    /**
     * 获取报警记录表单数据
     *
     * @param id 报警记录ID
     * @return 报警记录表单数据
     */
     AlertEventForm getAlertEventFormData(Long id);

    /**
     * 新增报警记录
     *
     * @param formData 报警记录表单对象
     * @return 是否新增成功
     */
    boolean saveAlertEvent(AlertEventForm formData);

    /**
     * 修改报警记录
     *
     * @param id   报警记录ID
     * @param formData 报警记录表单对象
     * @return 是否修改成功
     */
    boolean updateAlertEvent(Long id, AlertEventForm formData);

    /**
     * 删除报警记录
     *
     * @param ids 报警记录ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteAlertEvents(String ids);

}
