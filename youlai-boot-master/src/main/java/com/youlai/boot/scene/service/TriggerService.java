package com.youlai.boot.scene.service;

import com.youlai.boot.scene.model.entity.Trigger;
import com.youlai.boot.scene.model.form.TriggerForm;
import com.youlai.boot.scene.model.query.TriggerQuery;
import com.youlai.boot.scene.model.vo.TriggerVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 触发器服务类
 *
 * @author way
 * @since 2025-07-30 17:25
 */
public interface TriggerService extends IService<Trigger> {

    /**
     *触发器分页列表
     *
     * @return {@link IPage<TriggerVO>} 触发器分页列表
     */
    IPage<TriggerVO> getTriggerPage(TriggerQuery queryParams);

    /**
     * 获取触发器表单数据
     *
     * @param id 触发器ID
     * @return 触发器表单数据
     */
     TriggerForm getTriggerFormData(Long id);

    /**
     * 新增触发器
     *
     * @param formData 触发器表单对象
     * @return 是否新增成功
     */
    boolean saveTrigger(TriggerForm formData);

    /**
     * 修改触发器
     *
     * @param id   触发器ID
     * @param formData 触发器表单对象
     * @return 是否修改成功
     */
    boolean updateTrigger(Long id, TriggerForm formData);

    /**
     * 删除触发器
     *
     * @param ids 触发器ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteTriggers(String ids);

}
