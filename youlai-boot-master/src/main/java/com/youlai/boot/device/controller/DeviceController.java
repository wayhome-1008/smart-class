package com.youlai.boot.device.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.config.mqtt.MqttCallback;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.converter.DeviceConverter;
import com.youlai.boot.device.model.dto.GateWayManage;
import com.youlai.boot.device.model.dto.GateWayManageParams;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceForm;
import com.youlai.boot.device.model.query.DeviceQuery;
import com.youlai.boot.device.model.vo.DeviceVO;
import com.youlai.boot.device.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.youlai.boot.common.util.MacUtils.reParseMACAddress;
import static com.youlai.boot.config.mqtt.TopicConfig.BASE_TOPIC;
import static com.youlai.boot.config.mqtt.TopicConfig.TOPIC_LIST;

/**
 * 设备管理前端控制层
 *
 * @author way
 * @since 2025-05-08 15:16
 */
@Slf4j
@Tag(name = "04.设备管理接口")
@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MqttProducer mqttProducer;
    private final MqttCallback mqttCallback;
    private final DeviceConverter deviceConverter;

    @Operation(summary = "设备管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('device:device:query')")
    public PageResult<DeviceVO> getDevicePage(DeviceQuery queryParams) {
        IPage<DeviceVO> result = deviceService.getDevicePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "网关设备下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listGatewayOptions() {
        List<Option<Long>> list = deviceService.listGatewayOptions();
        return Result.success(list);
    }

    @Operation(summary = "新增设备管理")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('device:device:add')")
    public Result<Void> saveDevice(@RequestBody @Valid DeviceForm formData) throws MqttException {
        //校验MAC是否存在
        boolean isExist = deviceService.isExistDeviceMac(formData.getDeviceMac());
        if (isExist) {
            return Result.failed("设备已存在");
        }
        boolean result = false;
        //目前switch应该分ZigBee网关 ZigBee网关子设备 MQTT独立设备 WIFI独立设备
        //所以先根据通讯方式做不同处理
        switch (formData.getCommunicationModeItemId().intValue()) {
            case 1:
                //ZigBee网关子设备
                zigBeeDevice(formData);
                result = deviceService.saveDevice(formData);
                break;
            case 2:
                //WIFI独立设备
                wifiDevice(formData);
                result = deviceService.saveDevice(formData);
                break;
            case 3:
                //ZigBee网关
                zigBeeGateWay(formData);
                result = deviceService.saveDevice(formData);
                break;
            case 4:
                //MQTT独立设备
                mqttDevice(formData);
                result = deviceService.saveDevice(formData);
                break;
            default:
                break;
        }
        return Result.judge(result);
    }

    @Operation(summary = "获取设备管理表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('device:device:edit')")
    public Result<DeviceForm> getDeviceForm(
            @Parameter(description = "设备管理ID") @PathVariable Long id
    ) {
        DeviceForm formData = deviceService.getDeviceFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改设备管理")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('device:device:edit')")
    public Result<Void> updateDevice(
            @Parameter(description = "设备管理ID") @PathVariable Long id,
            @RequestBody @Validated DeviceForm formData
    ) {
        boolean result = deviceService.updateDevice(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "查询网关下子设备列表")
    @GetMapping("/subDevicePage")
    public PageResult<DeviceVO> getSubDevicePage(
            DeviceQuery queryParams
    ) {
        Device gateWay = deviceService.getById(queryParams.getId());
        if (gateWay == null) {
            throw new BusinessException("设备不存在");
        }
        if (gateWay.getDeviceTypeId() != 1) {
            throw new BusinessException("设备不是网关");
        }
        IPage<DeviceVO> result = deviceService.getSubDevicePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "删除设备管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('device:device:delete')")
    public Result<Void> deleteDevices(
            @Parameter(description = "设备管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) throws MqttException {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        boolean result = false;
        List<Device> devices = deviceService.listByIds(idList);
        for (Device device : devices) {
            switch (device.getCommunicationModeItemId().intValue()) {
                case 1:
                    //ZigBee网关子设备
                    zigBeeDeviceDel(device);
                    result = deviceService.removeById(device.getId());
                    gatewayDeviceDelMqtt(device);
                    break;
                case 2:
                    //WIFI独立设备
                    wifiDeviceDel(device);
                    result = deviceService.removeById(device.getId());
                    break;
                case 3:
                    //ZigBee网关
                    zigBeeGateWayDelDel(device);
                    result = deviceService.removeById(device.getId());
                case 4:
                    //MQTT独立设备
                    mqttDeviceDel(device);
                    result = deviceService.removeById(device.getId());
                    break;
                default:
                    break;
            }
        }

        return Result.judge(result);
    }

    private void mqttDeviceDel(Device device) {
        //删缓存
        redisTemplate.opsForHash().delete(RedisConstants.Device.DEVICE, device.getDeviceCode());
    }

    private void zigBeeGateWayDelDel(Device device) {
        //删缓存
        redisTemplate.opsForHash().delete(RedisConstants.Device.DEVICE, device.getDeviceCode());
    }

    private void wifiDeviceDel(Device device) {
        //删缓存
        redisTemplate.opsForHash().delete(RedisConstants.Device.DEVICE, device.getDeviceCode());
    }

    private void zigBeeDeviceDel(Device device) throws MqttException {
        //删缓存
        redisTemplate.opsForHash().delete(RedisConstants.Device.DEVICE, device.getDeviceCode());
        //实时订阅
        gatewayDeviceDelMqtt(device);
    }

    private void gatewayDeviceDelMqtt(Device device) throws MqttException {
        //查询网关
        Device gateway = deviceService.getById(device.getDeviceGatewayId());
        //1.构造消息
        // 构建最外层HashMap
        HashMap<String, Object> rootMap = new HashMap<>();
        // 设置sequence字段（Integer类型）
        rootMap.put("sequence", 123);
        // 设置cmd字段（String类型）
        rootMap.put("cmd", "delsubdevice");
        // 构建params内层HashMap
        HashMap<String, Object> paramsMap = new HashMap<>();
        // 构建devices数组（List<HashMap>类型）
        List<HashMap<String, String>> devicesList = new ArrayList<>();
        // 添加设备项（单个设备的HashMap）
        HashMap<String, String> deviceItem = new HashMap<>();
        deviceItem.put("deviceId", reParseMACAddress(device.getDeviceMac()));
        devicesList.add(deviceItem);  // 可添加多个设备项
        // 将devices数组放入paramsMap
        paramsMap.put("devices", devicesList);
        // 将paramsMap放入最外层rootMap
        rootMap.put("params", paramsMap);
        mqttProducer.send("/zbgw/" + gateway.getDeviceCode() + "/manage", 2, false, JSON.toJSONString(rootMap));
    }

    private void wifiDevice(@Valid DeviceForm formData) {
        log.info(formData.toString());
    }

    private void mqttDevice(@Valid DeviceForm formData) {
        mqttCallback.subscribeTopic("tele/" + formData.getDeviceMac() + "/SENSOR");
        mqttCallback.subscribeTopic("tele/" + formData.getDeviceMac() + "/STATE");
    }

    private void zigBeeDevice(@Valid DeviceForm formData) throws MqttException {
        //只要是zigBee网关子设备 第一步发送mqtt 然后存库 topic从绑定的网关开始
        Device gateway = deviceService.getById(formData.getDeviceGatewayId());
        if (ObjectUtils.isEmpty(gateway)) {
            throw new BusinessException("网关不存在");
        }
        gatewayDeviceAddMqtt(gateway.getDeviceCode());
    }

    private void zigBeeGateWay(@Valid DeviceForm formData) {
        //zigBee网关(需要去缓存根据mac查询是否存在)
        Device device = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, formData.getDeviceCode());
        if (ObjectUtils.isEmpty(device)) {
            //此处应该存Device
            Device entity = deviceConverter.toEntity(formData);
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, formData.getDeviceCode(), entity);
        }
        //实时订阅
        for (String consumerTopic : TOPIC_LIST) {
            mqttCallback.subscribeTopic(BASE_TOPIC + formData.getDeviceCode() + consumerTopic);
        }
    }

    private void gatewayDeviceAddMqtt(String macTopic) throws MqttException {
        //1.构造消息
        GateWayManageParams params = new GateWayManageParams();
        params.setPermitjoin(true);
        params.setAdddevtime(60);
        GateWayManage gateWayManage = new GateWayManage();
        gateWayManage.setSequence(RandomUtil.randomNumbers(3));
        gateWayManage.setCmd("addsubdevice");
        gateWayManage.setParams(params);
        //2.发送请求网关添加子设备
        mqttProducer.send("/zbgw/" + macTopic + "/manage", 2, false, JSON.toJSONString(gateWayManage));
    }
}
