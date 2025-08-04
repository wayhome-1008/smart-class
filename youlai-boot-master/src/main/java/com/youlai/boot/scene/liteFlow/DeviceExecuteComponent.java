package com.youlai.boot.scene.liteFlow;

import com.alibaba.fastjson.JSON;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.scene.model.entity.Action;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.service.ActionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-07-31  12:10
 *@Description: TODO
 */
@LiteflowComponent(id = "deviceExecute")
@Slf4j
public class DeviceExecuteComponent extends NodeComponent {
    @Autowired
    private MqttClient mqttClient;
    @Autowired
    private ActionService actionService;
    private static DeviceService deviceService = com.youlai.boot.common.util.SpringUtils.getBean(DeviceService.class);

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
            executeDeviceOperations(actions);
        }
    }

    private void executeDeviceOperations(List<Action> actions) {
        try {
            log.info("准备执行 {} 个动作", actions.size());

            for (Action action : actions) {
                List<String> deviceIds = Arrays.stream(action.getDeviceIds().split(","))
                        .filter(id -> !id.trim().isEmpty())
                        .toList();
                String parameters = action.getParameters();

                if (parameters != null && !parameters.isEmpty()) {
                    List<DeviceOperate> deviceOperates = JSON.parseArray(parameters, DeviceOperate.class);
                    log.info("准备执行动作，涉及 {} 个设备", deviceIds.size());

                    for (DeviceOperate deviceOperate : deviceOperates) {
                        for (String deviceId : deviceIds) {
                            try {
                                log.info("执行设备操作 - DeviceId: {}, Operate: {}", deviceId, deviceOperate.getOperate());
                                operate(deviceOperate, Long.valueOf(deviceId));
                                log.info("设备 {} 执行操作成功", deviceId);
                            } catch (Exception e) {
                                log.error("设备 {} 执行操作失败: {}", deviceId, e.getMessage(), e);
                            }
                        }
                    }
                } else {
                    log.warn("动作参数为空，跳过设备操作");
                }
            }
        } catch (Exception e) {
            log.error("执行设备操作时出错: {}", e.getMessage(), e);
        }
    }

    public void operate(DeviceOperate deviceOperate, Long deviceId) {
        Device device = deviceService.getById(deviceId);
        if (ObjectUtils.isEmpty(device)) return;
        if (device.getDeviceTypeId() != 4 && device.getDeviceTypeId() != 7 && device.getDeviceTypeId() != 10 && device.getDeviceTypeId() != 8)
            return;
        //根据通信协议去发送不同协议报文
        switch (CommunicationModeEnum.getNameById(device.getCommunicationModeItemId())) {
//            case "ZigBee" ->
            //zigBee
//                    zigBeeDevice(device.getDeviceCode(), device.getDeviceGatewayId(), deviceOperate.getOperate(), deviceOperate.getWay(), deviceOperate.getCount());
            case "WiFi" ->
                //WiFi
                    wifiDevice(device.getDeviceCode(), deviceOperate.getOperate(), deviceOperate.getWay(), deviceOperate.getCount());
            default -> Result.failed("暂不支持该协议");
        }
    }

    private void wifiDevice(String deviceCode, String operate, String way, Integer lightCount) {
        //目前能控制的就只有灯的开关
        log.info("正在发送~~~~~~~~~~");
        //判断几路
        if (lightCount == 1) {
            try {
                mqttClient.publish("cmnd/" + deviceCode + "/POWER", JSON.toJSONString(operate).getBytes(), 1, false);
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }

        } else if (way.equals("-1")) {
            try {
                for (int i = 1; i <= lightCount; i++) {
                    mqttClient.publish("cmnd/" + deviceCode + "/POWER" + i, JSON.toJSONString(operate).getBytes(), 1, false);
                }
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        } else {
            try {
                mqttClient.publish("cmnd/" + deviceCode + "/POWER" + way, JSON.toJSONString(operate).getBytes(), 1, false);
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        }
    }
}
