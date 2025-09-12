package com.youlai.boot.device.operation;

import com.youlai.boot.device.model.dto.Control;
import com.youlai.boot.device.model.dto.ControlParams;
import com.youlai.boot.device.model.dto.Switch;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.model.form.Operation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-09-12  10:06
 *@Description: TODO
 */
public class OperationUtils {
    private static final String ZIGBEE_TOPIC_PREFIX = "/zbgw/";
    private static final String ZIGBEE_TOPIC_SUFFIX = "/sub/control";
    private static final String WIFI_TOPIC_PREFIX = "cmnd/";
    private static final String WIFI_TOPIC_SUFFIX = "/POWER";

    public static String makeZigBeeTopic(String gateWayTopic) {
        return ZIGBEE_TOPIC_PREFIX + gateWayTopic + ZIGBEE_TOPIC_SUFFIX;
    }

    public static String makeWifiTopic(String deviceCode,String way) {
        return WIFI_TOPIC_PREFIX + deviceCode + WIFI_TOPIC_SUFFIX+way;
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

    public static DeviceOperate convert(Operation operation) {
        DeviceOperate deviceOperate = new DeviceOperate();
        deviceOperate.setOperate(operation.getOperate());
        deviceOperate.setWay(operation.getWay());
        deviceOperate.setCount(operation.getCount());
    }
}
