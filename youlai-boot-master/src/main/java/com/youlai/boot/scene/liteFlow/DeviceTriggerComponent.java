package com.youlai.boot.scene.liteFlow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.entity.Trigger;
import com.youlai.boot.scene.model.form.ThresholdCondition;
import com.youlai.boot.scene.service.TriggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    @Autowired
    private TriggerService triggerService;
    @Override
    public boolean isAccess() {
        // 默认返回true，实际触发条件在process方法中判断
        return true;
    }

    @Override
    public void process() throws Exception {
        Scene scene = this.getContextBean(Scene.class);
        // 正确的查询方式：使用Service层方法
        Trigger trigger = triggerService.getOne(new LambdaQueryWrapper<Trigger>()
                .eq(Trigger::getSceneId, scene.getId()));
            List<String> deviceIds = Arrays.stream(trigger.getDeviceIds().split(",")).toList();
            List<ThresholdCondition> conditions = trigger.getThresholdConditions();
            String logic = trigger.getThresholdLogic();
            // 检查设备条件并设置执行结果
            boolean triggerResult = checkTriggerResult(deviceIds, conditions, logic);

//        Trigger trigger = this.getContextBean(Trigger.class);
//        this.setOutput(triggerResult);
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
//            Object actualValue = deviceService.getProperty(deviceId, condition.getProperty());
            List<String> listMetric = deviceService.listMetric(Long.valueOf(deviceId));
            boolean conditionMet = false;
            for (String actualValue : listMetric) {
                conditionMet = ThresholdComparator.compare(condition, actualValue);
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
