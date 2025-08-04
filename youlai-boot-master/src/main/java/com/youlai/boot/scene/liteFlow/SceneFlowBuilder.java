package com.youlai.boot.scene.liteFlow;

import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.youlai.boot.scene.model.entity.Action;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.entity.Trigger;
import jakarta.annotation.Resource;
import com.yomahub.liteflow.flow.FlowBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *@Author: way
 *@CreateTime: 2025-07-31  09:48
 *@Description: TODO
 */
@Component
@Slf4j
public class SceneFlowBuilder {

    @Resource
    private FlowExecutor flowExecutor;

    /**
     * 为场景创建并注册流程
     */
    public void registerFlow(Scene scene) {
        String flowId = "scene_" + scene.getId();
        String elExpr = buildElExpression(scene);
        log.info("注册流程: {}", flowId);
        LiteFlowChainELBuilder.createChain().setChainId(flowId).setEL(
                elExpr).build();
    }

    /**
     * 构建场景的EL表达式
     */
    public String buildElExpression(Scene scene) {
        // 构建触发条件表达式
        String triggerExpr = buildTriggerExpr(scene);
        // 构建动作执行表达式
        String actionExpr = buildActionExpr(scene);
        // 完整流程：触发条件 -> 静默检查 -> 延时执行 -> 动作执行
        String elExpr = "THEN(" + triggerExpr + ", silenceCheck, delayExecute, " + actionExpr + ");";
        log.info("流程表达式: {}", elExpr);
        return elExpr;
    }

    /**
     * 构建触发条件表达式
     */
    private String buildTriggerExpr(Scene scene) {
        var triggers = scene.getTriggers();
        var conditionType = scene.getConditionType();

        if ("NOT".equals(conditionType)) {
            return "NOT(" + getTriggerCompId(triggers.get(0)) + ")";
        }

        var joiner = new StringBuilder(conditionType.equals("ALL") ? "THEN(" : "OR(");
        for (int i = 0; i < triggers.size(); i++) {
            joiner.append(getTriggerCompId(triggers.get(i)));
            if (i < triggers.size() - 1) {
                joiner.append(conditionType.equals("ALL") ? ", " : " || ");
            }
        }
        joiner.append(")");
        return joiner.toString();
    }

    /**
     * 构建动作执行表达式
     */
    private String buildActionExpr(Scene scene) {
        var actions = scene.getActions();
        var executeMode = scene.getExecuteMode();

        var joiner = new StringBuilder();
        for (int i = 0; i < actions.size(); i++) {
            joiner.append(getActionCompId(actions.get(i)));
            if (i < actions.size() - 1) {
                joiner.append(executeMode.equals("SERIAL") ? " -> " : " || ");
            }
        }
        return joiner.toString();
    }

    /**
     * 获取触发器组件ID
     */
    private String getTriggerCompId(Trigger trigger) {
        return switch (trigger.getType()) {
            case "DEVICE_TRIGGER" -> "deviceTrigger";
            case "PRODUCT_TRIGGER" -> "productTrigger";
            case "TIMER_TRIGGER" -> "timerTrigger";
            default -> throw new IllegalArgumentException("未知触发类型: " + trigger.getType());
        };
    }

    /**
     * 获取动作组件ID
     */
    private String getActionCompId(Action action) {
        return switch (action.getType()) {
            case "DEVICE_EXECUTE" -> "deviceExecute";
            case "PRODUCT_EXECUTE" -> "productExecute";
            case "ALARM_EXECUTE" -> "alarmExecute";
            default -> throw new IllegalArgumentException("未知动作类型: " + action.getType());
        };
    }
}
