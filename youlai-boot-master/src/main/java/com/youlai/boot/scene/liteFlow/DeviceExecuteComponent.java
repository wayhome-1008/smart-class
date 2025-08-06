package com.youlai.boot.scene.liteFlow;

import com.alibaba.fastjson.JSON;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.Result;
//import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.model.dto.Control;
import com.youlai.boot.device.model.dto.Switch;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.scene.model.entity.Action;
import com.youlai.boot.scene.model.entity.Scene;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.youlai.boot.device.controller.DeviceOperateController.makeControlParams;

/**
 *@Author: way
 *@CreateTime: 2025-07-31  12:10
 *@Description: TODO
 */
@LiteflowComponent(id = "deviceExecute", name = "设备执行组件")
@Slf4j
@RequiredArgsConstructor
public class DeviceExecuteComponent extends NodeComponent {
    //    @Autowired
    private final RedisTemplate<String, Object> redisTemplate;

    //    @Autowired
//    private MqttClient mqttClient;
//    private final MqttProducer  mqttProducer;
    private static final DeviceService deviceService = com.youlai.boot.common.util.SpringUtils.getBean(DeviceService.class);

    @Override
    public void process() throws Exception {
        Scene scene = this.getContextBean(Scene.class);
        log.info("获取场景信息，ID: {}", scene.getId());
        MqttClient mqttClient = this.getContextBean(MqttClient.class);
        // 直接从scene中获取Action列表
        List<Action> actions = scene.getActions();
        if (actions == null || actions.isEmpty()) {
            log.warn("场景 {} 未配置动作", scene.getId());
            this.setIsEnd(true);
            return;
        }

        if (scene.getSilenceTime() != null && scene.getSilenceTime() == 0) {
            executeDeviceOperations(actions, mqttClient);
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
                                operate(deviceOperate, deviceCode, mqttClient);
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

    public void operate(DeviceOperate deviceOperate, String deviceCode, MqttClient mqttClient) throws MqttException {
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
        if (ObjectUtils.isEmpty(device)) return;
        if (device.getDeviceTypeId() != 4 && device.getDeviceTypeId() != 7 && device.getDeviceTypeId() != 10 && device.getDeviceTypeId() != 8)
            return;
        //根据通信协议去发送不同协议报文
        switch (CommunicationModeEnum.getNameById(device.getCommunicationModeItemId())) {
            case "ZigBee" ->
//            zigBee
                    zigBeeDevice(device.getDeviceCode(), device.getDeviceGatewayId(), deviceOperate.getOperate(), deviceOperate.getWay(), deviceOperate.getCount(), mqttClient);
            case "WiFi" ->
                //WiFi
                    wifiDevice(device.getDeviceCode(), deviceOperate.getOperate(), deviceOperate.getWay(), deviceOperate.getCount(), mqttClient);
            default -> Result.failed("暂不支持该协议");
        }
    }

    private void zigBeeDevice(String deviceCode, Long deviceGatewayId, @Pattern(regexp = "ON|OFF", message = "错误操作") String operate, String way, Integer count, MqttClient mqttClient) throws MqttException {
        log.info("zigBeeDevice直接出发");
        Control control = makeControl(deviceCode);
        Switch plug = makeSwitch(operate, Integer.parseInt(way) - 1);
        List<Switch> switches = new ArrayList<>();
        switches.add(plug);
        makeControlParams(switches, control);
        mqttClient.publish("/zbgw/" + "9454c5ee8180" + "/sub/control", JSON.toJSONString(control).getBytes(), 2, false);
    }

    @NotNull
    private static Switch makeSwitch(String operate, int i) {
        Switch plug = new Switch();
        plug.setSwitchStatus(operate.equals("ON") ? "on" : "off");
        plug.setOutlet(i);
        return plug;
    }

    @NotNull
    private static Control makeControl(String deviceCode) {
        Control control = new Control();
        control.setDeviceId(deviceCode);
        control.setSequence((int) System.currentTimeMillis());
        return control;
    }

    private void wifiDevice(String deviceCode, String operate, String way, Integer lightCount, MqttClient mqttClient) {
        //目前能控制的就只有灯的开关
        log.info("正在发送~~~~~~~~~~");
        //判断几路
        if (lightCount == 1) {
            try {
                mqttClient.publish("cmnd/" + deviceCode + "/POWER", operate.getBytes(), 1, false);
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }

        } else if (way.equals("-1")) {
            try {
                for (int i = 1; i <= lightCount; i++) {
                    mqttClient.publish("cmnd/" + deviceCode + "/POWER" + i, operate.getBytes(), 1, false);
                }
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        } else {
            try {
                mqttClient.publish("cmnd/" + deviceCode + "/POWER" + way, operate.getBytes(), 1, false);
                String topic = "cmnd/" + deviceCode + "/POWER" + way;
                String payload = JSON.toJSONString(operate);
                log.info("发送MQTT消息 - 主题: {}, 内容: {}", topic, payload);
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        }
        this.setIsEnd(true);
    }
}
