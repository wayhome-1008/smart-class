package com.youlai.boot.system.service;

import com.youlai.boot.system.model.entity.AlertRule;
import com.youlai.boot.system.model.form.AlertRuleForm;
import com.youlai.boot.system.model.query.AlertRuleQuery;
import com.youlai.boot.system.model.vo.AlertRuleVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 报警配置服务类
 *
 * @author way
 * @since 2025-07-21 11:22
 */
public interface AlertRuleService extends IService<AlertRule> {

    /**
     *报警配置分页列表
     *
     * @return {@link IPage<AlertRuleVO>} 报警配置分页列表
     */
    IPage<AlertRuleVO> getAlertRulePage(AlertRuleQuery queryParams);

    /**
     * 获取报警配置表单数据
     *
     * @param id 报警配置ID
     * @return 报警配置表单数据
     */
     AlertRuleForm getAlertRuleFormData(Long id);

    /**
     * 新增报警配置
     *
     * @param formData 报警配置表单对象
     * @return 是否新增成功
     */
    boolean saveAlertRule(AlertRuleForm formData);

    /**
     * 修改报警配置
     *
     * @param id   报警配置ID
     * @param formData 报警配置表单对象
     * @return 是否修改成功
     */
    boolean updateAlertRule(Long id, AlertRuleForm formData);

    /**
     * 删除报警配置
     *
     * @param ids 报警配置ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteAlertRules(String ids);

}
