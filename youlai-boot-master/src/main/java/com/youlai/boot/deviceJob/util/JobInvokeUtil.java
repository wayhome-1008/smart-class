package com.youlai.boot.deviceJob.util;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.device.operation.DeviceOperation;
import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.List;

/**
 * 任务执行工具
 *
 * @author kerwincui
 */
@Slf4j
public class JobInvokeUtil {

    /**获取消息推送接口*/
    private static final MqttClient messagePublish = com.youlai.boot.common.util.SpringUtils.getBean(MqttClient.class);
    private static final DeviceOperation deviceOperation = com.youlai.boot.common.util.SpringUtils.getBean(DeviceOperation.class);

    /**
     * 执行方法
     *
     * @param deviceJob 系统任务
     */
    public static void invokeMethod(DeviceJob deviceJob) throws Exception {
        System.out.println("------------------------执行定时任务-----------------------------");
        if (deviceJob.getStatus() == 1) {
            // 统一使用 parseArray 处理
            List<DeviceOperate> deviceOperates = JSON.parseArray(deviceJob.getActions(), DeviceOperate.class);
            for (DeviceOperate deviceOperate : deviceOperates) {
                deviceOperation.operateForSchedule(deviceOperate, deviceJob.getDeviceId(), messagePublish);
            }
        }
    }
}