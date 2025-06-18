package com.youlai.boot.device.handler.zigbee;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.model.dto.reportSubDevice.ReportSubDevice;
import com.youlai.boot.device.model.dto.reportSubDevice.SubDevice;
import com.youlai.boot.device.model.dto.reportSubDevice.rsp.ReportSubDeviceRsp;
import com.youlai.boot.device.model.dto.reportSubDevice.rsp.ReportSubDeviceRspParams;
import com.youlai.boot.device.model.dto.reportSubDevice.rsp.Results;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.topic.HandlerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.youlai.boot.common.util.MacUtils.extractFromTopic;
import static com.youlai.boot.device.schedule.ApiMonitorService.deviceRequestTimeMap;
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
    private final DeviceService deviceService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void process(String topic, String jsonMsg, MqttClient mqttClient) {
        try {
            log.info("[网关上线后上报子设备:{}]", jsonMsg);
            //网关上线后上报子设备
            ReportSubDevice reportSubDevice = JSON.parseObject(jsonMsg, ReportSubDevice.class);
            List<SubDevice> reportSubDevices = reportSubDevice.getParams().getSubDevices();
            ReportSubDeviceRsp reportSubDeviceRsp = getReportSubDeviceRsp(reportSubDevice, reportSubDevices);
            mqttClient.publish(topic + "_rsp", JSON.toJSONString(reportSubDeviceRsp).getBytes(), 2, false);
            //更新网关map
            String deviceCode = extractFromTopic(topic);
            Device gateway = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, deviceCode);
            if (gateway != null) {
                gateway.setStatus(1);
                redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, deviceCode, gateway);
                deviceService.updateById(gateway);
                gateway.setDeviceLastDate(new java.util.Date());
                deviceRequestTimeMap.put(deviceCode, gateway);
            }
            //对网关报道的设备设置在线 其余离线
            for (SubDevice subDevice : reportSubDevices) {
                String originalMac = subDevice.getDeviceId();
                Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, originalMac);
                if (deviceCache != null) {
                    deviceCache.setStatus(1);
                    redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, originalMac, deviceCache);
                    deviceService.updateById(deviceCache);
                } else {
                    //查库
                    deviceCache = deviceService.getByCode(originalMac);
                    if (deviceCache != null) {
                        deviceCache.setStatus(1);
                        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, originalMac, deviceCache);
                        deviceService.updateById(deviceCache);
                    }
                }
            }
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    @NotNull
    private static ReportSubDeviceRsp getReportSubDeviceRsp(ReportSubDevice reportSubDevice, List<SubDevice> reportSubDevices) {
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
        return reportSubDeviceRsp;
    }

    @Override
    public HandlerType getType() {
        return HandlerType.REPORT_SUBDEVICE;
    }
}
