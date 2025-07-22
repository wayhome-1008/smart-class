package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.common.constant.RedisConstants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.system.mapper.AlertRuleMapper;
import com.youlai.boot.system.service.AlertRuleService;
import com.youlai.boot.system.model.entity.AlertRule;
import com.youlai.boot.system.model.form.AlertRuleForm;
import com.youlai.boot.system.model.query.AlertRuleQuery;
import com.youlai.boot.system.model.vo.AlertRuleVO;
import com.youlai.boot.system.converter.AlertRuleConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 报警配置服务实现类
 *
 * @author way
 * @since 2025-07-21 11:22
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule> implements AlertRuleService {

    private final AlertRuleConverter alertRuleConverter;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 初始化报警配置缓存
     */
    @PostConstruct
    public void initAlertRuleCache() {
        log.info("初始化报警配置缓存... ");
        refreshAlertRuleCache();
    }

    private void refreshAlertRuleCache() {
        // 清理报警配置缓存
        redisTemplate.delete(RedisConstants.Alert.Alert);

        List<AlertRule> alertRules = this.list();
        if (CollectionUtil.isNotEmpty(alertRules)) {
            alertRules.forEach(item -> {
                // 构建复合key: deviceId:metric
                String cacheKey = item.getDeviceId() + ":" + item.getMetricKey();
                redisTemplate.opsForHash().put(
                        RedisConstants.Alert.Alert,
                        cacheKey,
                        item
                );
            });
        }
    }

    // 查询方法示例
    public AlertRule getAlertRuleByDeviceAndMetric(String deviceId, String metric) {
        String cacheKey = deviceId + ":" + metric;
        return (AlertRule) redisTemplate.opsForHash().get(RedisConstants.Alert.Alert, cacheKey);
    }


    /**
     * 获取报警配置分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<AlertRuleVO>} 报警配置分页列表
     */
    @Override
    public IPage<AlertRuleVO> getAlertRulePage(AlertRuleQuery queryParams) {
        Page<AlertRuleVO> pageVO = this.baseMapper.getAlertRulePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }

    /**
     * 获取报警配置表单数据
     *
     * @param id 报警配置ID
     * @return 报警配置表单数据
     */
    @Override
    public AlertRuleForm getAlertRuleFormData(Long id) {
        AlertRule entity = this.getById(id);
        return alertRuleConverter.toForm(entity);
    }

    /**
     * 新增报警配置
     *
     * @param formData 报警配置表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveAlertRule(AlertRuleForm formData) {
        AlertRule entity = alertRuleConverter.toEntity(formData);
        //同步缓存
        refreshAlertRuleCache();
        return this.save(entity);
    }

    /**
     * 更新报警配置
     *
     * @param id   报警配置ID
     * @param formData 报警配置表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateAlertRule(Long id, AlertRuleForm formData) {
        AlertRule entity = alertRuleConverter.toEntity(formData);
        //同步缓存
        refreshAlertRuleCache();
        return this.updateById(entity);
    }

    /**
     * 删除报警配置
     *
     * @param ids 报警配置ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteAlertRules(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的报警配置数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        //同步缓存
        refreshAlertRuleCache();
        return this.removeByIds(idList);
    }

}
