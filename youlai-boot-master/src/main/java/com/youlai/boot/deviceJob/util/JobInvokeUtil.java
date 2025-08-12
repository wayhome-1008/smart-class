package com.youlai.boot.deviceJob.util;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.model.dto.Control;
import com.youlai.boot.device.model.dto.ControlParams;
import com.youlai.boot.device.model.dto.Switch;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务执行工具
 *
 * @author kerwincui
 */
@Slf4j
public class JobInvokeUtil {

    /**获取消息推送接口*/
    private static MqttClient messagePublish = com.youlai.boot.common.util.SpringUtils.getBean(MqttClient.class);
    private static DeviceService deviceService = com.youlai.boot.common.util.SpringUtils.getBean(DeviceService.class);

    /**
     * 执行方法
     *
     * @param deviceJob 系统任务
     */
    public static void invokeMethod(DeviceJob deviceJob) throws Exception {
        System.out.println("------------------------执行定时任务-----------------------------");
        if (deviceJob.getJobType() == 1) {
//          // 统一使用 parseArray 处理
            List<DeviceOperate> deviceOperates = JSON.parseArray(deviceJob.getActions(), DeviceOperate.class);
            for (DeviceOperate deviceOperate : deviceOperates) {
                operate(deviceOperate, deviceJob.getDeviceId());
            }
        }
    }

    public static void operate(DeviceOperate deviceOperate, Long deviceId) {
        Device device = deviceService.getById(deviceId);
        if (ObjectUtils.isEmpty(device)) return;
        if (device.getDeviceTypeId() != 4 && device.getDeviceTypeId() != 7 && device.getDeviceTypeId() != 10 && device.getDeviceTypeId() != 8)
            return;
        //根据通信协议去发送不同协议报文
        switch (CommunicationModeEnum.getNameById(device.getCommunicationModeItemId())) {
            case "ZigBee" ->
//            zigBee
                    zigBeeDevice(device.getDeviceCode(), device.getDeviceGatewayId(), deviceOperate.getOperate(), deviceOperate.getWay(), deviceOperate.getCount());
            case "WiFi" ->
                //WiFi
                    wifiDevice(device.getDeviceCode(), deviceOperate.getOperate(), deviceOperate.getWay(), deviceOperate.getCount());
            default -> Result.failed("暂不支持该协议");
        }
    }

    private static Result<Void> zigBeeDevice(String deviceCode, Long deviceGatewayId, String operate, String way, Integer lightCount) {
        //目前能控制的就只有计量插座和开关
        //查询该子设备的网关
        Device gateway = deviceService.getById(deviceGatewayId);
        if (ObjectUtils.isEmpty(gateway)) return Result.failed("该设备没有网关");
        //组发送json
        if (way.equals("-1")) {
            for (int i = 0; i < lightCount; i++) {
                Control control = makeControl(deviceCode);
                Switch plug = makeSwitch(operate, i);
                List<Switch> switches = new ArrayList<>();
                switches.add(plug);
                makeControlParams(switches, control);
                String deviceMac = gateway.getDeviceMac();
                String gateWayTopic = MacUtils.reParseMACAddress(deviceMac);
                try {
                    messagePublish.publish("/zbgw/" + gateWayTopic + "/sub/control", JSON.toJSONString(control).getBytes(), 0, false);
                } catch (MqttException e) {
                    log.error(e.getMessage());
                }
            }
        } else {
            Control control = makeControl(deviceCode);
            Switch plug = makeSwitch(operate, Integer.parseInt(way) - 1);
            List<Switch> switches = new ArrayList<>();
            switches.add(plug);
            makeControlParams(switches, control);
            String deviceMac = gateway.getDeviceMac();
            String gateWayTopic = MacUtils.reParseMACAddress(deviceMac);
            log.info(control.toString());
            try {
                messagePublish.publish("/zbgw/" + gateWayTopic + "/sub/control", JSON.toJSONString(control).getBytes(), 0, false);
            } catch (MqttException e) {
                log.error(e.getMessage());
            }
        }
        return Result.success();

    }

    private static void wifiDevice(String deviceCode, String operate, String way, Integer lightCount) {
        //目前能控制的就只有灯的开关
        log.info("正在发送~~~~~~~~~~");
        //判断几路
        if (lightCount == 1) {
            try {
                messagePublish.publish("cmnd/" + deviceCode + "/POWER", operate.getBytes(), 1, false);
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }

        } else if (way.equals("-1")) {
            try {
                for (int i = 1; i <= lightCount; i++) {
                    messagePublish.publish("cmnd/" + deviceCode + "/POWER" + i, operate.getBytes(), 1, false);
                }
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        } else {
            try {
                messagePublish.publish("cmnd/" + deviceCode + "/POWER" + way, JSON.toJSONString(operate).getBytes(), 1, false);
            } catch (MqttException e) {
                log.error("发送消息失败", e);
            }
        }
    }

    public static void makeControlParams(List<Switch> switches, Control control) {
        ControlParams controlParams = new ControlParams();
        controlParams.setSwitches(switches);
        control.setParams(controlParams);
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
}