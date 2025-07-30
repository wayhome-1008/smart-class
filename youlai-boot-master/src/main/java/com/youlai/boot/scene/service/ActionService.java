package com.youlai.boot.scene.service;

import com.youlai.boot.scene.model.entity.Action;
import com.youlai.boot.scene.model.form.ActionForm;
import com.youlai.boot.scene.model.query.ActionQuery;
import com.youlai.boot.scene.model.vo.ActionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 执行器服务类
 *
 * @author way
 * @since 2025-07-30 17:28
 */
public interface ActionService extends IService<Action> {

    /**
     *执行器分页列表
     *
     * @return {@link IPage<ActionVO>} 执行器分页列表
     */
    IPage<ActionVO> getActionPage(ActionQuery queryParams);

    /**
     * 获取执行器表单数据
     *
     * @param id 执行器ID
     * @return 执行器表单数据
     */
     ActionForm getActionFormData(Long id);

    /**
     * 新增执行器
     *
     * @param formData 执行器表单对象
     * @return 是否新增成功
     */
    boolean saveAction(ActionForm formData);

    /**
     * 修改执行器
     *
     * @param id   执行器ID
     * @param formData 执行器表单对象
     * @return 是否修改成功
     */
    boolean updateAction(Long id, ActionForm formData);

    /**
     * 删除执行器
     *
     * @param ids 执行器ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteActions(String ids);

}
