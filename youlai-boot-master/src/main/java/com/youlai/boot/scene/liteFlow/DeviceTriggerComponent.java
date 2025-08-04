package com.youlai.boot.scene.liteFlow;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.entity.Trigger;
import com.youlai.boot.scene.model.form.ThresholdCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
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

    @Override
    public boolean isAccess() {
        // 默认返回true，实际触发条件在process方法中判断
        return true;
    }

    @Override
    public void process() throws Exception {
        Scene scene = this.getContextBean(Scene.class);

        // 直接从scene中获取Trigger列表
        List<Trigger> triggers = scene.getTriggers();
        if (triggers == null || triggers.isEmpty()) {
            log.warn("场景 {} 未配置触发器", scene.getId());
            this.setIsEnd(true);
            return;
        }

        // 遍历所有触发器进行检查
        boolean triggerResult = false;
        for (Trigger trigger : triggers) {
            List<String> deviceIds = Arrays.stream(trigger.getDeviceIds().split(","))
                    .filter(id -> !id.trim().isEmpty())
                    .toList();
            List<ThresholdCondition> conditions = trigger.getThresholdConditions();
            String logic = trigger.getThresholdLogic();

            // 检查设备条件
            triggerResult = checkTriggerResult(deviceIds, conditions, logic);

            // 如果任一触发器满足条件，则触发场景
            if (triggerResult) {
                break;
            }
        }

        // 如果没有触发条件满足，则结束流程
        if (!triggerResult) {
            this.setIsEnd(true);
        }
    }

    private boolean checkTriggerResult(List<String> deviceIds, List<ThresholdCondition> conditions, String logic) {
        for (String deviceId : deviceIds) {
            if (checkDeviceConditions(deviceId, conditions, logic)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkDeviceConditions(String deviceId, List<ThresholdCondition> conditions, String logic) {
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        for (ThresholdCondition condition : conditions) {
            List<String> listMetric = deviceService.listMetric(Long.valueOf(deviceId));
            boolean conditionMet = false;
            for (String actualValue : listMetric) {
                conditionMet = ThresholdComparator.compare(condition, actualValue);
                if (conditionMet) {
                    break; // 找到一个满足条件的值即可
                }
            }

            if ("AND".equals(logic) && !conditionMet) {
                return false;
            }
            if ("OR".equals(logic) && conditionMet) {
                return true;
            }
        }
        return "AND".equals(logic);
    }

}
