package com.youlai.boot.scene.liteFlow;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.scene.model.entity.Scene;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.stereotype.Service;

/**
 * 场景触发执行服务（封装所有场景触发逻辑）
 */
@Slf4j
@Service
public class SceneExecuteService {
    @Resource
    private FlowExecutor flowExecutor;

    /**
     * 执行单个场景的核心方法（内部封装）
     */
    public boolean executeScene(Scene scene, Device device, MqttClient mqttClient, ObjectNode allSwitchStates) {
        String flowId = "scene_" + scene.getId();
        try {
            // 1. 准备上下文参数
            // 2. 执行LiteFlow流程
            LiteflowResponse response = flowExecutor.execute2Resp(
                    flowId,  // 流程ID（与注册时一致）
                    null,    // 初始参数（无则传null）
                    scene,     // 上下文对象
                    device, mqttClient, allSwitchStates
            );
            // 3. 处理执行结果
            if (response.isSuccess()) {
//                log.info("场景[{}:{}]正常执行", scene.getId(), scene.getSceneName());
                return true;
            } else {
                log.error("场景[{}:{}]执行失败：{}",
                        scene.getId(), scene.getSceneName(), response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("场景[{}:{}]执行抛出异常", scene.getId(), scene.getSceneName(), e);
            return false;
        }
    }

//    /**
//     * 判断场景是否包含定时触发器
//     */
//    private boolean hasTimerTrigger(Scene scene) {
//        return scene.getTriggers().stream()
//                .anyMatch(trigger -> "TIMER_TRIGGER".equals(trigger.getType()));
//    }
}
