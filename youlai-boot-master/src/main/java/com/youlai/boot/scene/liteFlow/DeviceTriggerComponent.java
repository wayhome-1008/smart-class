package com.youlai.boot.scene.liteFlow;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.entity.Trigger;
import com.youlai.boot.scene.model.form.ThresholdCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@LiteflowComponent(id = "deviceTrigger")
@Slf4j
public class DeviceTriggerComponent extends NodeComponent {
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean isAccess() {
        // 默认返回true，实际触发条件在process方法中判断
        return true;
    }

    @Override
    public void process() throws Exception {
        Scene scene = this.getContextBean(Scene.class);
        Device triggerDevice = this.getContextBean(Device.class);
        // 直接从scene中获取Trigger列表
        List<Trigger> triggers = scene.getTriggers();
        if (triggers == null || triggers.isEmpty()) {
            log.warn("场景 {} 未配置触发器", scene.getId());
            this.setIsEnd(true);
            return;
        }

        // 检查是否有任何一个触发器被满足
        boolean triggerResult = false;
        for (Trigger trigger : triggers) {
            List<String> deviceCodes = Arrays.stream(trigger.getDeviceCodes().split(","))
                    .filter(id -> !id.trim().isEmpty())
                    .toList();
            // 关键检查：当前触发设备是否在该触发器的设备列表中
            if (!deviceCodes.contains(triggerDevice.getDeviceCode())) {
                continue; // 不在该触发器设备列表中，跳过检查
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
            String logic = trigger.getThresholdLogic();

            // 检查该触发器是否满足条件
            triggerResult = checkSingleTrigger(triggerDevices, conditions, logic);

            // 如果任一触发器满足条件，则触发场景
            if (triggerResult) {
                log.info("场景 {} 被设备 {} 触发", scene.getId(), triggerDevice.getDeviceCode());
                // 一旦触发成功，立即返回，进入下一个流程组件
                return;
            }
        }
        // 如果没有触发条件满足，则结束流程
        if (!triggerResult) {
            log.warn("场景 {} 未触发", scene.getId());
            this.setIsEnd(true);
        }
    }

    /**
     * 检查单个触发器是否满足条件
     * @param devices 该触发器关联的所有设备
     * @param conditions 触发条件
     * @param logic 逻辑关系(AND/OR)
     * @return 是否满足触发条件
     */
    private boolean checkSingleTrigger(List<Device> devices, List<ThresholdCondition> conditions, String logic) {
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        // 对于每个条件，检查是否至少有一个设备满足该条件
        if ("AND".equals(logic)) {
            // AND逻辑：所有条件都必须被至少一个设备满足
            for (ThresholdCondition condition : conditions) {
                boolean conditionMet = false;
                // 检查是否有设备满足当前条件
                for (Device device : devices) {
                    if (checkConditionForDevice(device, condition)) {
                        conditionMet = true;
                        break;
                    }
                }
                // 如果有任何一个条件没有被满足，则整个触发器不满足
                if (!conditionMet) {
                    return false;
                }
            }
            return true; // 所有条件都被满足
        } else {
            // OR逻辑：任一条件被至少一个设备满足即可
            for (ThresholdCondition condition : conditions) {
                // 检查是否有设备满足当前条件
                for (Device device : devices) {
                    if (checkConditionForDevice(device, condition)) {
                        return true; // 找到满足条件的设备，立即返回true
                    }
                }
            }
            return false; // 没有任何条件被满足
        }
    }

    /**
     * 检查单个设备是否满足特定条件
     * @param device 设备对象
     * @param condition 条件
     * @return 是否满足条件
     */
    private boolean checkConditionForDevice(Device device, ThresholdCondition condition) {
        try {
            JsonNode deviceInfo = device.getDeviceInfo();
            if (deviceInfo.has(condition.getProperty())) {
                String propertyValue = deviceInfo.get(condition.getProperty()).asText();
                boolean result = ThresholdComparator.compare(condition, propertyValue);
                log.debug("设备 {} 属性 {} 值为 {}，条件 {} {}，结果: {}",
                        device.getDeviceName(), condition.getProperty(), propertyValue,
                        condition.getOperator(), condition.getValue(), result);
                return result;
            }
        } catch (Exception e) {
            log.error("检查设备 {} 条件时出错", device.getDeviceName(), e);
        }
        return false;
    }
}