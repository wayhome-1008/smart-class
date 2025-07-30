package com.youlai.boot.scene.liteFlow;

import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.scene.model.entity.Trigger;
import com.youlai.boot.scene.model.form.ThresholdCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-07-30  17:56
 *@Description: TODO
 */
@Component("deviceTrigger")
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
        Trigger trigger = this.getContextBean(Trigger.class);
        List<String> deviceIds = Arrays.stream(trigger.getDeviceIds().split(",")).toList();
        List<ThresholdCondition> conditions = trigger.getThresholdConditions();
        String logic = trigger.getThresholdLogic();
        // 检查设备条件并设置执行结果
        boolean triggerResult = checkTriggerResult(deviceIds, conditions, logic);
        this.setOutput(triggerResult);
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
        if (conditions.isEmpty()) {
            return false;
        }

        for (ThresholdCondition condition : conditions) {
            Object actualValue = deviceService.getProperty(deviceId, condition.getProperty());
            boolean conditionMet = ThresholdComparator.compare(condition, actualValue);

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
