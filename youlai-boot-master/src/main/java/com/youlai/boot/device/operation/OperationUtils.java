package com.youlai.boot.device.operation;

import com.youlai.boot.device.model.dto.Control;
import com.youlai.boot.device.model.dto.ControlParams;
import com.youlai.boot.device.model.dto.Switch;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.model.form.Operation;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-09-12  10:06
 *@Description: TODO
 */
@Slf4j
public class OperationUtils {
     static final String ZIGBEE_TOPIC_PREFIX = "/zbgw/";
     static final String ZIGBEE_TOPIC_SUFFIX = "/sub/control";
     static final String WIFI_TOPIC_PREFIX = "cmnd/";
     static final String WIFI_TOPIC_SUFFIX = "/POWER";

    public static String makeZigBeeTopic(String gateWayTopic) {
        String topic = ZIGBEE_TOPIC_PREFIX + gateWayTopic + ZIGBEE_TOPIC_SUFFIX;
        log.info("生成ZigBee主题: {}", topic);
        return topic;
    }

    public static String makeWifiTopic(String deviceCode, String way) {
        String topic = WIFI_TOPIC_PREFIX + deviceCode + WIFI_TOPIC_SUFFIX + way;
        log.info("生成WiFi主题: {}", topic);
        return topic;
    }

    @NotNull
    public static Switch makeSwitch(String operate, int i) {
        Switch plug = new Switch();
        plug.setSwitchStatus(operate.equals("ON") ? "on" : "off");
        plug.setOutlet(i);
        return plug;
    }

    @NotNull
    public static Control makeControl(String deviceCode) {
        Control control = new Control();
        control.setDeviceId(deviceCode);
        control.setSequence((int) System.currentTimeMillis());
        return control;
    }

    public static void makeControlParams(List<Switch> switches, Control control) {
        ControlParams controlParams = new ControlParams();
        controlParams.setSwitches(switches);
        control.setParams(controlParams);
    }

    public static DeviceOperate convert(Operation operation, Integer count, String way) {
        DeviceOperate deviceOperate = new DeviceOperate();
        deviceOperate.setOperate(operation.getOperate());
        deviceOperate.setWay(way);
        deviceOperate.setCount(count);
        return deviceOperate;
    }
}
