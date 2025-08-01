package com.youlai.boot.scene.liteFlow;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.config.mqtt.MqttProducer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *@Author: way
 *@CreateTime: 2025-07-31  12:10
 *@Description: TODO
 */
@LiteflowComponent(id = "deviceExecute")
public class DeviceExecuteComponent extends NodeComponent {
    @Autowired
    private  MqttProducer mqttProducer;

    @Override
    public void process() throws Exception {
//        Scene scene = this.getContextBean(Scene.class);
//        Action action = actionService.getOne(new LambdaQueryWrapper<Action>()
//                .eq(Action::getSceneId, scene.getId()));
//        List<String> deviceIds = Arrays.stream(action.getDeviceIds().split(",")).toList();
//        String parameters = action.getParameters();
//        List<DeviceOperate> deviceOperates = JSON.parseArray(parameters, DeviceOperate.class);
//        for (DeviceOperate deviceOperate : deviceOperates) {
//            for (String deviceId : deviceIds) {
//                messagePublish.operate(deviceOperate, Long.valueOf(deviceId));
//            }
//        }

    }
}
