package com.youlai.boot.scene.liteFlow;

import com.alibaba.fastjson.JSON;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.operation.DeviceOperation;
import com.youlai.boot.scene.model.entity.Action;
import com.youlai.boot.scene.model.entity.Scene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;

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

    @Override
    public void process() throws Exception {
        Scene scene = this.getContextBean(Scene.class);
        log.info("获取场景信息，ID: {}", scene.getId());
        // 直接从scene中获取Action列表
        List<Action> actions = scene.getActions();
        if (actions == null || actions.isEmpty()) {
            log.warn("场景 {} 未配置动作", scene.getId());
            this.setIsEnd(true);
            return;
        }
        if (scene.getSilenceTime() != null && scene.getSilenceTime() == 0) {
            executeDeviceOperations(actions, this.getContextBean(MqttClient.class));
        }
    }

    private void executeDeviceOperations(List<Action> actions, MqttClient mqttClient) {
        try {
            log.info("准备执行 {} 个动作", actions.size());

            for (Action action : actions) {
                List<String> deviceCodes = Arrays.stream(action.getDeviceCodes().split(","))
                        .filter(id -> !id.trim().isEmpty())
                        .toList();
                String parameters = action.getParameters();

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
                } else {
                    log.warn("动作参数为空，跳过设备操作");
                }
            }
            this.setIsEnd(true);
        } catch (Exception e) {
            log.error("执行设备操作时出错: {}", e.getMessage(), e);
        }
    }
}
