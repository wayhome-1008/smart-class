package com.youlai.boot.device.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.model.dto.GateWayManage;
import com.youlai.boot.device.model.dto.GateWayManageParams;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceForm;
import com.youlai.boot.device.model.query.DeviceQuery;
import com.youlai.boot.device.model.vo.DeviceVO;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.deviceType.mapper.DeviceTypeMapper;
import com.youlai.boot.deviceType.model.entity.DeviceType;
import com.youlai.boot.system.mapper.DictItemMapper;
import com.youlai.boot.system.model.entity.DictItem;
import com.youlai.boot.system.service.DictItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.youlai.boot.common.util.MacUtils.reParseMACAddress;
import static com.youlai.boot.device.handler.SubUpdateHandler.deviceList;

/**
 * 设备管理前端控制层
 *
 * @author way
 * @since 2025-05-08 15:16
 */
@Tag(name = "设备管理接口")
@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
@Transactional
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceTypeMapper deviceTypeMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MqttProducer mqttProducer;

    @Operation(summary = "设备管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('device:device:query')")
    public PageResult<DeviceVO> getDevicePage(DeviceQuery queryParams) {
        IPage<DeviceVO> result = deviceService.getDevicePage(queryParams);
        return PageResult.success(result);
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
        boolean result = deviceService.saveDevice(formData);
        //不同设备类型需要做不同处
        DeviceType deviceType = deviceTypeMapper.selectById(formData.getDeviceTypeId());
        switch (deviceType.getDeviceType()) {
            case "网关":
                gateWay(formData);
                break;
            case "光照传感器":
                sensor(formData);
                break;
            case "计量插座":
                processPlug(formData);
        }
        return Result.judge(result);
    }

    private void processPlug(DeviceForm formData) throws MqttException {
        //1.构造消息
        GateWayManageParams params = new GateWayManageParams();
        params.setPermitjoin(true);
        params.setAdddevtime(60);
        GateWayManage gateWayManage = new GateWayManage();
        gateWayManage.setSequence(RandomUtil.randomNumbers(3));
        gateWayManage.setCmd("addsubdevice");
        gateWayManage.setParams(params);
        //2.发送请求网关添加子设备
        mqttProducer.send("/zbgw/9454c5ee8180/manage", 2, false, JSON.toJSONString(gateWayManage));
    }

    private void sensor(DeviceForm formData) throws MqttException {
        //1.构造消息
        GateWayManageParams params = new GateWayManageParams();
        params.setPermitjoin(true);
        params.setAdddevtime(60);
        GateWayManage gateWayManage = new GateWayManage();
        gateWayManage.setSequence(RandomUtil.randomNumbers(3));
        gateWayManage.setCmd("addsubdevice");
        gateWayManage.setParams(params);
        //2.发送请求网关添加子设备
        mqttProducer.send("/zbgw/9454c5ee8180/manage", 2, false, JSON.toJSONString(gateWayManage));
    }

    private void gateWay(DeviceForm formData) {
        //根據mac查詢redis中是否存在
        Object object = redisTemplate.opsForHash().get(RedisConstants.MqttDevice.DEVICE, MacUtils.parseMACAddress(formData.getDeviceMac()));
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

    @Operation(summary = "删除设备管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('device:device:delete')")
    public Result<Void> deleteDevices(
            @Parameter(description = "设备管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) throws MqttException {
        Device byId = deviceService.getById(ids);
        boolean result = deviceService.deleteDevices(ids);
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
        deviceItem.put("deviceId", reParseMACAddress(byId.getDeviceMac()));
        devicesList.add(deviceItem);  // 可添加多个设备项

        // 将devices数组放入paramsMap
        paramsMap.put("devices", devicesList);

        // 将paramsMap放入最外层rootMap
        rootMap.put("params", paramsMap);

        //2.发送请求网关添加子设备
        mqttProducer.send("/zbgw/9454c5ee8180/manage", 2, false, JSON.toJSONString(rootMap));
        return Result.judge(result);
    }

    //一些redis接口 先現實吧 後續再更新
    @GetMapping("/deviceInfo")
    public Result<List<JSONObject>> deviceInfo() {
        List<JSONObject> wheels = new ArrayList<>();
        for (Device device : deviceList) {
            //去掉网关的
            if (device.getDeviceTypeId()==1) {
                continue;
            }
            Object object = redisTemplate.opsForHash().get(RedisConstants.MqttDevice.DEVICE, device.getDeviceCode());
            if (ObjectUtils.isNotEmpty(object)) {
                wheels.add(JSONObject.parseObject(JSON.toJSONString(object)));
            }
        }
        return Result.success(wheels);
    }
}
