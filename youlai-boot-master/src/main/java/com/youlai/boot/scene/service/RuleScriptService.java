package com.youlai.boot.scene.service;

import com.youlai.boot.scene.model.entity.RuleScript;
import com.youlai.boot.scene.model.form.RuleScriptForm;
import com.youlai.boot.scene.model.query.RuleScriptQuery;
import com.youlai.boot.scene.model.vo.RuleScriptVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 规则引擎脚本服务类
 *
 * @author way
 * @since 2025-07-29 11:54
 */
public interface RuleScriptService extends IService<RuleScript> {

    /**
     *规则引擎脚本分页列表
     *
     * @return {@link IPage<RuleScriptVO>} 规则引擎脚本分页列表
     */
    IPage<RuleScriptVO> getRuleScriptPage(RuleScriptQuery queryParams);

    /**
     * 获取规则引擎脚本表单数据
     *
     * @param id 规则引擎脚本ID
     * @return 规则引擎脚本表单数据
     */
     RuleScriptForm getRuleScriptFormData(Long id);

    /**
     * 新增规则引擎脚本
     *
     * @param formData 规则引擎脚本表单对象
     * @return 是否新增成功
     */
    boolean saveRuleScript(RuleScriptForm formData);

    /**
     * 修改规则引擎脚本
     *
     * @param id   规则引擎脚本ID
     * @param formData 规则引擎脚本表单对象
     * @return 是否修改成功
     */
    boolean updateRuleScript(Long id, RuleScriptForm formData);

    /**
     * 删除规则引擎脚本
     *
     * @param ids 规则引擎脚本ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteRuleScripts(String ids);

}
