package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.system.model.entity.AlertRule;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.system.model.query.AlertRuleQuery;
import com.youlai.boot.system.model.vo.AlertRuleVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报警配置Mapper接口
 *
 * @author way
 * @since 2025-07-21 11:22
 */
@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRule> {

    /**
     * 获取报警配置分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<AlertRuleVO>} 报警配置分页列表
     */
    Page<AlertRuleVO> getAlertRulePage(Page<AlertRuleVO> page, AlertRuleQuery queryParams);

}
