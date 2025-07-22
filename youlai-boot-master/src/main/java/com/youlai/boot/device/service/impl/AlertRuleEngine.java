package com.youlai.boot.device.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.alertEvent.model.entity.AlertEvent;
import com.youlai.boot.alertEvent.service.AlertEventService;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.system.model.entity.AlertRule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *@Author: way
 *@CreateTime: 2025-07-22  16:51
 *@Description: 报警规则引擎 - 核心处理逻辑
 */
@Component
@RequiredArgsConstructor
public class AlertRuleEngine {
    // 缓存设备报警历史(deviceId:metric -> List<AlertEvent>)
    private final Map<String, List<AlertEvent>> deviceAlarmHistory = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final AlertEventService alertEventService;

    /**
     * 检查规则是否触发报警（带时间窗口处理）
     */
    public boolean checkRule(AlertRule rule, Long currentValue) {
        // 1. 基础条件检查
        boolean conditionMet = checkBasicCondition(rule, currentValue);
        if (!conditionMet) {
            return false;
        }

        // 2. 时间窗口检查
        return checkTimeWindow(rule, currentValue);
    }

    private boolean checkBasicCondition(AlertRule rule, Long currentValue) {
        String compareType = rule.getCompareType();
        return switch (compareType) {
            case ">" -> currentValue.compareTo(rule.getThresholdValue()) > 0;
            case "<" -> currentValue.compareTo(rule.getThresholdValue()) < 0;
            case ">=" -> currentValue.compareTo(rule.getThresholdValue()) >= 0;
            case "<=" -> currentValue.compareTo(rule.getThresholdValue()) <= 0;
            case "==" -> currentValue.compareTo(rule.getThresholdValue()) == 0;
            case "!=" -> currentValue.compareTo(rule.getThresholdValue()) != 0;
            case "range" -> currentValue.compareTo(rule.getMinValue()) < 0 ||
                    currentValue.compareTo(rule.getMaxValue()) > 0;
            default -> false;
        };
    }

    private boolean checkTimeWindow(AlertRule rule, Long currentValue) {
        // 如果没有设置时间窗口，直接返回true
        if (rule.getTimeWindow() == null || rule.getTimeWindow() <= 0) {
            return true;
        }

        String historyKey = rule.getDeviceId() + ":" + rule.getMetricKey();
        List<AlertEvent> history = deviceAlarmHistory.computeIfAbsent(historyKey, k -> new CopyOnWriteArrayList<>());

        // 清理过期记录
        cleanExpiredRecords(history, rule.getTimeWindow());

        // 添加当前记录
        AlertEvent currentEvent = new AlertEvent();
        currentEvent.setEventTime(System.currentTimeMillis());
        currentEvent.setCurrentValue(currentValue);
        history.add(currentEvent);

        // 检查是否达到触发次数
        return history.size() >= rule.getConsecutiveCount();
    }

    private void cleanExpiredRecords(List<AlertEvent> history, Integer timeWindowSeconds) {
        long currentTime = System.currentTimeMillis();
        history.removeIf(event ->
                (currentTime - event.getEventTime()) > timeWindowSeconds * 1000L
        );
    }

    /**
     * 构建报警事件（带时间窗口处理）
     */
    public void constructAlertEvent(Device device, AlertRule rule, JsonNode metrics) {
        if (!shouldTriggerAlert(rule)) {
            return;
        }

        AlertEvent alertEvent = new AlertEvent();
        alertEvent.setRuleId(rule.getId());
        alertEvent.setDeviceId(device.getId());
        alertEvent.setMetricKey(rule.getMetricKey());
        alertEvent.setCurrentValue(metrics.get(rule.getMetricKey()).asLong());
        alertEvent.setAlarmContent(rule.getRuleName());
        alertEvent.setLevel(rule.getLevel());
        alertEvent.setStatus("0"); // 未处理状态
        alertEvent.setEventTime(System.currentTimeMillis());

        alertEventService.save(alertEvent);
        updateAlarmHistory(rule, alertEvent);
    }

    private boolean shouldTriggerAlert(AlertRule rule) {
        // 如果没有设置时间窗口，直接返回true
        if (rule.getTimeWindow() == null || rule.getTimeWindow() <= 0) {
            return true;
        }

        String historyKey = rule.getDeviceId() + ":" + rule.getMetricKey();
        List<AlertEvent> history = deviceAlarmHistory.get(historyKey);

        return history != null && history.size() >= rule.getConsecutiveCount();
    }

    private void updateAlarmHistory(AlertRule rule, AlertEvent event) {
        String historyKey = rule.getDeviceId() + ":" + rule.getMetricKey();
        deviceAlarmHistory.computeIfAbsent(historyKey, k -> new CopyOnWriteArrayList<>())
                .add(event);
    }

    /**
     * 检查设备报警配置
     * @param deviceId 设备ID
     * @param params 上报的参数对象
     * @return 匹配的报警规则对象，如果没有则返回null
     */
    public AlertRule checkAlertConfig(Long deviceId, JsonNode params) {
        if (params == null || params.isNull() || params.isEmpty()) {
            return null;
        }

        // 获取第一个参数名作为指标名称
        Iterator<String> fieldNames = params.fieldNames();
        if (!fieldNames.hasNext()) {
            return null;
        }

        String metric = fieldNames.next();
        String alertKey = deviceId + ":" + metric;  // 组合键格式 deviceId:metric

        // 从Redis查询并返回报警配置
        Object alertRuleObj = redisTemplate.opsForHash().get(RedisConstants.Alert.Alert, alertKey);
        if (alertRuleObj instanceof AlertRule) {
            return (AlertRule) alertRuleObj;
        }
        return null;
    }
}

