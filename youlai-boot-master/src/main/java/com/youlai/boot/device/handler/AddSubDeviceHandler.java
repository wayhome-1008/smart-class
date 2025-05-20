package com.youlai.boot.device.handler;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.AddSubDevice;
import com.youlai.boot.device.model.form.AddSubDeviceRsp;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-04-27  14:42
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AddSubDeviceHandler implements MsgHandler {
    private final DeviceService deviceService;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        try {
            //网关请求添加子设备
            log.info("[接收到网关请求添加子设备消息:{}]", jsonMsg);
            AddSubDevice addSubDevice = JSON.parseObject(jsonMsg, AddSubDevice.class);
            AddSubDevice.Params addSubDeviceParams = addSubDevice.getParams();
            List<AddSubDevice.SubDevices> subDevices = addSubDeviceParams.getSubDevices();
            AddSubDeviceRsp.Params params = new AddSubDeviceRsp.Params();
            AddSubDeviceRsp addSubDeviceRsp = new AddSubDeviceRsp();
            addSubDeviceRsp.setError(0);
            addSubDeviceRsp.setSequence(addSubDevice.getSequence());
            List<AddSubDeviceRsp.Results> results1 = new ArrayList<>();
            for (AddSubDevice.SubDevices subDevice : subDevices) {
                //数据库中存在才可以发送
                Device device = deviceService.getByMac(MacUtils.parseMACAddress(subDevice.getMac()));
                if (device == null) {
                    continue;
                }
                AddSubDeviceRsp.Results results = new AddSubDeviceRsp.Results();
                results.setError(0);
                results.setDeviceId(subDevice.getMac());
                results.setMac(subDevice.getMac());
                results1.add(results);
                params.setResults(results1);
                addSubDeviceRsp.setParams(params);
                log.info("发送消息:{}", addSubDeviceRsp);
                mqttClient.publish(topic + "_rsp", JSON.toJSONString(addSubDeviceRsp).getBytes(), 2, false);
                //同时把mac地址去掉：后存储在设备中
                device.setDeviceNo(subDevice.getMac());
                deviceService.updateById(device);
                log.info("添加设备成功");
            }
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    @Override
    public HandlerType getType() {
        return HandlerType.ADD_SUB_DEVICE;
    }
}
