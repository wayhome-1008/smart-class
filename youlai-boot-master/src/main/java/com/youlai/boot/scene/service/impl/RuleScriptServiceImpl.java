package com.youlai.boot.scene.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.scene.mapper.RuleScriptMapper;
import com.youlai.boot.scene.service.RuleScriptService;
import com.youlai.boot.scene.model.entity.RuleScript;
import com.youlai.boot.scene.model.form.RuleScriptForm;
import com.youlai.boot.scene.model.query.RuleScriptQuery;
import com.youlai.boot.scene.model.vo.RuleScriptVO;
import com.youlai.boot.scene.converter.RuleScriptConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 规则引擎脚本服务实现类
 *
 * @author way
 * @since 2025-07-29 11:54
 */
@Service
@RequiredArgsConstructor
public class RuleScriptServiceImpl extends ServiceImpl<RuleScriptMapper, RuleScript> implements RuleScriptService {

    private final RuleScriptConverter ruleScriptConverter;

    /**
    * 获取规则引擎脚本分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<RuleScriptVO>} 规则引擎脚本分页列表
    */
    @Override
    public IPage<RuleScriptVO> getRuleScriptPage(RuleScriptQuery queryParams) {
        Page<RuleScriptVO> pageVO = this.baseMapper.getRuleScriptPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取规则引擎脚本表单数据
     *
     * @param id 规则引擎脚本ID
     * @return 规则引擎脚本表单数据
     */
    @Override
    public RuleScriptForm getRuleScriptFormData(Long id) {
        RuleScript entity = this.getById(id);
        return ruleScriptConverter.toForm(entity);
    }
    
    /**
     * 新增规则引擎脚本
     *
     * @param formData 规则引擎脚本表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveRuleScript(RuleScriptForm formData) {
        RuleScript entity = ruleScriptConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新规则引擎脚本
     *
     * @param id   规则引擎脚本ID
     * @param formData 规则引擎脚本表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateRuleScript(Long id,RuleScriptForm formData) {
        RuleScript entity = ruleScriptConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除规则引擎脚本
     *
     * @param ids 规则引擎脚本ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteRuleScripts(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的规则引擎脚本数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
