package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.system.converter.AlertRuleConverter;
import com.youlai.boot.system.mapper.AlertRuleMapper;
import com.youlai.boot.system.model.entity.AlertRule;
import com.youlai.boot.system.model.form.AlertRuleForm;
import com.youlai.boot.system.model.query.AlertRuleQuery;
import com.youlai.boot.system.model.vo.AlertRuleVO;
import com.youlai.boot.system.service.AlertRuleService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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

    /**
     * 获取报警配置分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<AlertRuleVO>} 报警配置分页列表
     */
    @Override
    public IPage<AlertRuleVO> getAlertRulePage(AlertRuleQuery queryParams) {
        return this.baseMapper.getAlertRulePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
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
        String cacheKey = formData.getDeviceId() + ":" + formData.getMetricKey();
        AlertRule existingRule = (AlertRule) redisTemplate.opsForHash().get(RedisConstants.Alert.Alert, cacheKey);

        // 校验：如果该设备该属性已经有报警配置则不允许新增
        if (existingRule != null) {
            log.warn("设备ID:{}的属性:{}已存在报警配置，不允许重复新增", formData.getDeviceId(), formData.getMetricKey());
            throw new RuntimeException("该设备该属性已有报警配置，不允许重复新增");
        }
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
        // 查询原有记录
        AlertRule oldRule = this.getById(id);
        if (oldRule == null) {
            log.warn("报警配置ID:{}不存在", id);
            return false;
        }

        // 如果设备ID或指标键有变更，需要校验新组合是否已存在
        if (!oldRule.getDeviceId().equals(formData.getDeviceId()) ||
                !oldRule.getMetricKey().equals(formData.getMetricKey())) {

            String newCacheKey = formData.getDeviceId() + ":" + formData.getMetricKey();
            AlertRule existingRule = (AlertRule) redisTemplate.opsForHash().get(RedisConstants.Alert.Alert, newCacheKey);

            // 校验：如果新设备新属性已经有报警配置则不允许修改为该组合
            if (existingRule != null && !existingRule.getId().equals(id)) {
                log.warn("设备ID:{}的属性:{}已存在报警配置，不允许修改为该组合", formData.getDeviceId(), formData.getMetricKey());
                throw new RuntimeException("目标设备目标属性已有报警配置，不允许重复配置");
            }
        }
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
