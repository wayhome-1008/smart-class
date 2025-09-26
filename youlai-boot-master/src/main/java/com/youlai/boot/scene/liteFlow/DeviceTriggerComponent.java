package com.youlai.boot.scene.liteFlow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.util.DateUtils;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.deviceJob.service.DeviceJobService;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.entity.Trigger;
import com.youlai.boot.scene.model.form.ThresholdCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-07-30  17:56
 *@Description: TODO
 */
@LiteflowComponent(id = "deviceTrigger", name = "设备触发器组件")
@Slf4j
@RequiredArgsConstructor
public class DeviceTriggerComponent extends NodeComponent {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceJobService deviceJobService;

    @Override
    public boolean isAccess() {
        // 默认返回true，实际触发条件在process方法中判断
        return true;
    }

    @Override
    public void process() throws Exception {
        Scene scene = this.getContextBean(Scene.class);
        Device triggerDevice = this.getContextBean(Device.class);
        ObjectNode metrics = this.getContextBean(ObjectNode.class);

        // 直接从scene中获取Trigger列表
        List<Trigger> triggers = scene.getTriggers();
        if (triggers == null || triggers.isEmpty()) {
            log.info("场景 {} 未配置触发器", scene.getId());
            this.setIsEnd(true);
            return;
        }

        // 根据conditionType判断触发逻辑
        String conditionType = scene.getConditionType();
        boolean sceneTriggered;

        if ("ALL".equals(conditionType)) {
            // ALL逻辑：所有触发器都必须满足
            sceneTriggered = checkAllTriggers(triggers, triggerDevice, scene, metrics);
        } else if ("ANY".equals(conditionType)) {
            // ANY逻辑：任意触发器满足即可
            sceneTriggered = checkAnyTrigger(triggers, triggerDevice, scene, metrics);
        } else if ("NOT".equals(conditionType)) {
            // NOT逻辑：所有触发器都不满足
            sceneTriggered = !checkAnyTrigger(triggers, triggerDevice, scene, metrics);
        } else {
            // 默认使用ANY逻辑
            sceneTriggered = checkAnyTrigger(triggers, triggerDevice, scene, metrics);
        }

        // 如果没有触发条件满足，则结束流程
        if (!sceneTriggered) {
            this.setIsEnd(true);
        }
    }

    /**
     * 检查是否所有触发器都满足条件 (ALL逻辑)
     */
    private boolean checkAllTriggers(List<Trigger> triggers, Device triggerDevice, Scene scene, ObjectNode metrics) {
        for (Trigger trigger : triggers) {
            if (!isTriggerSatisfied(trigger, triggerDevice, metrics)) {
                return false; // 任何一个触发器不满足就返回false
            }
        }
        log.info("场景 {} 被设备 {} 触发 (ALL条件满足)", scene.getId(), triggerDevice.getDeviceCode());
        return true; // 所有触发器都满足
    }

    /**
     * 检查是否有任意触发器满足条件 (ANY逻辑)
     */
    private boolean checkAnyTrigger(List<Trigger> triggers, Device triggerDevice, Scene scene, ObjectNode metrics) throws SchedulerException {
        for (Trigger trigger : triggers) {
            if (trigger.getType().equals("TIMER_TRIGGER") && scene.getEnable() == 1) {
                //如果有定时触发 那么这里新增 因为新增有重复会删除旧的 所以无所谓
                // 1.创建任务
                deviceJobService.createScheduleJobForScene(scene);
                break;
            }
            if (isTriggerSatisfied(trigger, triggerDevice, metrics)) {
                log.info("场景 {} 被设备 {} 触发 (ANY条件满足)", scene.getId(), triggerDevice.getDeviceCode());
                return true; // 任一触发器满足就返回true
            }
        }
        return false; // 没有触发器满足
    }

    /**
     * 判断单个触发器是否满足条件
     * 修改为AND逻辑：触发器内所有条件都必须满足
     */
    private boolean isTriggerSatisfied(Trigger trigger, Device triggerDevice, ObjectNode metrics) {
        List<String> deviceCodes = Arrays.stream(trigger.getDeviceCodes().split(","))
                .filter(id -> !id.trim().isEmpty())
                .toList();

        // 关键检查：当前触发设备是否在该触发器的设备列表中
        if (!deviceCodes.contains(triggerDevice.getDeviceCode())) {
            return false; // 不在该触发器设备列表中，不满足条件
        }

        // 获取该触发器涉及的所有设备
        List<Device> triggerDevices = new ArrayList<>();
        for (String deviceCode : deviceCodes) {
            Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (device != null) {
                triggerDevices.add(device);
            }
        }

        List<ThresholdCondition> conditions = trigger.getThresholdConditions();
        //新增功能 时间范围触发(仅限当天的时间范围)
        if ("TIME_RANGE".equals(trigger.getType())) {
            //只在value存了时间范围
            ThresholdCondition thresholdCondition = conditions.get(0);
            String timeRange = (String) thresholdCondition.getValue();
            boolean checkTimeRangeTrigger = DateUtils.checkTimeRangeTrigger(timeRange);
            log.info("时间范围触发结果: {}", checkTimeRangeTrigger);
            return checkTimeRangeTrigger;
        } else {
            // 修改为AND逻辑：触发器内所有条件都必须满足
            for (ThresholdCondition condition : conditions) {
                boolean conditionMet = false;
                // 检查是否有设备满足当前条件
                for (Device device : triggerDevices) {
                    if (checkConditionForDevice(device, condition, metrics)) {
                        conditionMet = true;
                        break;
                    }
                }
                // 如果有任何一个条件没有被满足，则整个触发器不满足
                if (!conditionMet) {
                    this.setIsEnd(true);
                    log.info("场景 {} 未被设备 {} 触发", trigger.getSceneId(), triggerDevice.getDeviceCode());
                    return false;
                } else {
                    return true; // 所有条件都被满足
                }
            }
        }
        log.info("场景 {} 被设备 {} 触发", trigger.getSceneId(), triggerDevice.getDeviceCode());
        return true; // 所有条件都被满足
    }

    /**
     * 检查单个设备是否满足特定条件
     * @param device 设备对象
     * @param condition 条件
     * @return 是否满足条件
     */
    private boolean checkConditionForDevice(Device device, ThresholdCondition condition, ObjectNode metrics) {
        try {
            JsonNode deviceInfo = device.getDeviceInfo();
            //校验metrics是否和condition的条件相同 才进行下边逻辑
            Iterator<String> fieldNames = metrics.fieldNames();
            while (fieldNames.hasNext()) {
                //校验当前属性值和上次这个属性值是否相同
                String fieldName = fieldNames.next();
                //当前设备属性和触发的属性相同
                if (fieldName.equals(condition.getProperty())) {
                    //设备属性值
                    JsonNode nowMetricNode = metrics.get(fieldName);
                    //上次设备属性值
                    JsonNode propertyValueNode = deviceInfo.get(condition.getProperty());
                    // 添加空值检查
                    if (nowMetricNode == null || propertyValueNode == null) {
                        // 根据业务需求处理空值情况，例如跳过或记录日志
                        log.info("设备属性值为空，设备: {}, 属性: {}", device.getDeviceName(), condition.getProperty());
                        this.setIsEnd(true);
                        return false;
                    }

                    String nowMetric = nowMetricNode.asText();
                    String propertyValue = propertyValueNode.asText();

                    if (nowMetric.equals(propertyValue)) {
                        //说明这次的属性值和上次的属性值相同 不进行后续
                        log.info("属性值相同,则场景不执行,原值为{},新值为{}", propertyValue, nowMetric);
                        this.setIsEnd(true);
                        return false;
                    }
                    boolean result = ThresholdComparator.compare(condition, nowMetric);
                    log.info("设备 {} 属性 {} 值为 {}，条件 {} {}，结果: {}",
                            device.getDeviceName(), condition.getProperty(), propertyValue,
                            condition.getOperator(), condition.getValue(), result);
                    return !result;
                }

            }

        } catch (Exception e) {
            log.error("检查设备 {} 条件时出错", device.getDeviceName(), e);
        }
        return false;
    }
}