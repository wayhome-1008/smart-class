package com.youlai.boot.device.handler.zigbee;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.dto.reportSubDevice.ReportSubDevice;
import com.youlai.boot.device.model.dto.reportSubDevice.SubDevice;
import com.youlai.boot.device.model.dto.reportSubDevice.rsp.ReportSubDeviceRsp;
import com.youlai.boot.device.model.dto.reportSubDevice.rsp.ReportSubDeviceRspParams;
import com.youlai.boot.device.model.dto.reportSubDevice.rsp.Results;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.youlai.boot.device.topic.HandlerType.REPORT_SUBDEVICE;

/**
 *@Author: way
 *@CreateTime: 2025-04-27  11:49
 *@Description: TODO
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReportSubDeviceHandler implements MsgHandler {
    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        try {
            log.info("[接收到网关请求添加子设备消息:{}]", jsonMsg);
            //网关上线后上报子设备
            ReportSubDevice reportSubDevice = JSON.parseObject(jsonMsg, ReportSubDevice.class);
            List<SubDevice> reportSubDevices = reportSubDevice.getParams().getSubDevices();
            //返回
            ReportSubDeviceRsp reportSubDeviceRsp = new ReportSubDeviceRsp();
            reportSubDeviceRsp.setError(0);
            reportSubDeviceRsp.setSequence(reportSubDevice.getSequence());
            ReportSubDeviceRspParams params = new ReportSubDeviceRspParams();
            List<Results> resultsList = new ArrayList<>();
            for (SubDevice subDevice : reportSubDevices) {
                Results results = new Results();
                results.setDeviceId(subDevice.getDeviceId());
                results.setError(0);
                resultsList.add(results);
            }
            params.setResults(resultsList);
            reportSubDeviceRsp.setParams(params);
            mqttClient.publish(topic + "_rsp", JSON.toJSONString(reportSubDeviceRsp).getBytes(), 2, false);
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    @Override
    public HandlerType getType() {
        return REPORT_SUBDEVICE;
    }
}
