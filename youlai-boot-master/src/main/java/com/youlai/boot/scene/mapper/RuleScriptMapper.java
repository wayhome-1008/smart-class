package com.youlai.boot.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.scene.model.entity.RuleScript;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.scene.model.query.RuleScriptQuery;
import com.youlai.boot.scene.model.vo.RuleScriptVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则引擎脚本Mapper接口
 *
 * @author way
 * @since 2025-07-29 11:54
 */
@Mapper
public interface RuleScriptMapper extends BaseMapper<RuleScript> {

    /**
     * 获取规则引擎脚本分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<RuleScriptVO>} 规则引擎脚本分页列表
     */
    Page<RuleScriptVO> getRuleScriptPage(Page<RuleScriptVO> page, RuleScriptQuery queryParams);

}
