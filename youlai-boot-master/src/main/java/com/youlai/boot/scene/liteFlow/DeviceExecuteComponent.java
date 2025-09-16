package com.youlai.boot.scene.liteFlow;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.operation.DeviceOperation;
import com.youlai.boot.device.service.impl.AlertRuleEngine;
import com.youlai.boot.scene.model.entity.Action;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.system.model.entity.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-07-31  12:10
 *@Description: TODO
 */
@LiteflowComponent(id = "deviceExecute", name = "设备执行组件")
@Slf4j
@RequiredArgsConstructor
public class DeviceExecuteComponent extends NodeComponent {
    private final DeviceOperation deviceOperation;
    private final AlertRuleEngine alertRuleEngine;

    @Override
    public void process() throws Exception {
        //上下文获取需要的数据
        Scene scene = this.getContextBean(Scene.class);
        Device device = this.getContextBean(Device.class);
        //若场景绑定执行器为设备告警则需要校验设备上传的数据
        ObjectNode metrics = this.getContextBean(ObjectNode.class);
        log.info("获取场景信息，ID: {}", scene.getId());
        // 直接从scene中获取Action列表
        List<Action> actions = scene.getActions();
        if (actions == null || actions.isEmpty()) {
            log.warn("场景 {} 未配置动作", scene.getId());
            this.setIsEnd(true);
            return;
        }
        if (scene.getSilenceTime() != null && scene.getSilenceTime() == 0) {
            executeDeviceOperations(actions, device, metrics, this.getContextBean(MqttClient.class));
        }
    }

    private void executeDeviceOperations(List<Action> actions, Device device, ObjectNode metrics, MqttClient mqttClient) {
        try {
            log.info("准备执行 {} 个动作", actions.size());
            for (Action action : actions) {
                List<String> deviceCodes = Arrays.stream(action.getDeviceCodes().split(","))
                        .filter(id -> !id.trim().isEmpty())
                        .toList();
                String parameters = action.getParameters();
                //校验是否为设备执行动作
                if (action.getType().equals("DEVICE_EXECUTE")) {
                    if (parameters != null && !parameters.isEmpty()) {
                        List<DeviceOperate> deviceOperates = JSON.parseArray(parameters, DeviceOperate.class);
                        log.info("准备执行动作，涉及 {} 个设备", deviceCodes.size());

                        for (DeviceOperate deviceOperate : deviceOperates) {
                            for (String deviceCode : deviceCodes) {
                                try {
                                    log.info("执行设备操作 - DeviceId: {}, Operate: {}", deviceCode, deviceOperate.getOperate());
                                    deviceOperation.operateForScene(deviceOperate, deviceCode, mqttClient);
                                    log.info("设备 {} 执行操作成功", deviceCode);
                                } catch (Exception e) {
                                    log.error("设备 {} 执行操作失败: {}", deviceCode, e.getMessage(), e);
                                }
                            }
                        }
                    }
                }
                //校验是否是设备报警触发
                if (action.getType().equals("ALARM_EXECUTE")) {
                    //根据设备id+设备上报属性查询报警配置
                    //校验警报配置
                    AlertRule alertRule = alertRuleEngine.checkAlertConfig(device.getId(), metrics);
                    if (ObjectUtils.isNotEmpty(alertRule)) {
                        boolean checkRule = alertRuleEngine.checkRule(alertRule, metrics.get(alertRule.getMetricKey()).asLong());
                        //满足条件
                        if (checkRule) {
                            //创建AlertEvent
                            alertRuleEngine.constructAlertEvent(device, alertRule, metrics);
                        }
                    }
                }

            }
            this.setIsEnd(true);
        } catch (Exception e) {
            log.error("执行设备操作时出错: {}", e.getMessage(), e);
        }
    }
}
