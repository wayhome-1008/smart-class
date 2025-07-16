package com.youlai.boot.device.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;
import com.youlai.boot.categoryDeviceRelationship.service.CategoryDeviceRelationshipService;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.base.BasePageQuery;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.MacUtils;
import com.youlai.boot.config.mqtt.MqttCallback;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.Enum.DeviceTypeEnum;
import com.youlai.boot.device.converter.DeviceConverter;
import com.youlai.boot.device.factory.DeviceInfoParserFactory;
import com.youlai.boot.device.model.dto.GateWayManage;
import com.youlai.boot.device.model.dto.GateWayManageParams;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceForm;
import com.youlai.boot.device.model.query.DeviceQuery;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceMasterVO;
import com.youlai.boot.device.model.vo.DeviceVO;
import com.youlai.boot.device.service.DeviceInfoParser;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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
    private final CategoryDeviceRelationshipService categoryDeviceRelationshipService;
    private final CategoryService categoryService;
    private final RoomService roomService;

    @Operation(summary = "设备管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('device:device:query')")
    public PageResult<DeviceVO> getDevicePage(DeviceQuery queryParams) {
        IPage<DeviceVO> result = deviceService.getDevicePage(queryParams);
        //用redis得info替换
        Map<Object, Object> device = redisTemplate.opsForHash().entries(RedisConstants.Device.DEVICE);
        Map<String, Device> resultMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : device.entrySet()) {
            // 假设key可以安全转换为String
            String deviceCode = entry.getKey().toString();
            // 假设value就是Device对象，直接强制类型转换
            Device deviceObj = (Device) entry.getValue();
            resultMap.put(deviceCode, deviceObj);
        }
        result.getRecords().forEach(item -> {
            Device deviceInfo = resultMap.get(item.getDeviceCode());
            if (deviceInfo != null) {
                item.setDeviceInfo(deviceInfo.getDeviceInfo());
            }
        });
        if (result.getTotal() == 0) {
            return PageResult.success(result);
        }
        result.getRecords().forEach(item -> {
            //使用工厂对设备具体信息转换
            // 动态获取解析器
            // 使用枚举获取类型名称
            if (item.getDeviceTypeId() != 1) {
                String deviceType = DeviceTypeEnum.getNameById(item.getDeviceTypeId());
                String communicationMode = CommunicationModeEnum.getNameById(item.getCommunicationModeItemId());
                DeviceInfoParser parser = DeviceInfoParserFactory.getParser(deviceType, communicationMode);
                List<DeviceInfo> deviceInfos = parser.parse(item.getDeviceInfo());
                item.setDeviceInfoList(deviceInfos);
            }
        });
        //根据设备类型去对是否开启icon显示
        result.getRecords().forEach(d -> {
            d.setIsOpenIcon(false);
            if (d.getDeviceTypeId() != null) {
                //4->计量插座 7->开关 10->智能插座
                if (d.getDeviceTypeId() == 4 || d.getDeviceTypeId() == 7 || d.getDeviceTypeId() == 10) {
                    d.setIsOpenIcon(true);
                    //获取开关状态
                    Optional<Integer> count = DeviceInfo.getValueByName(d.getDeviceInfoList(), "count", Integer.class);
                    if (count.isPresent()) {
                        boolean found = false;
                        for (int i = 0; i < count.get() && !found; i++) {
                            Optional<String> switchStatus = DeviceInfo.getValueByName(
                                    d.getDeviceInfoList(),
                                    "switch" + (i + 1),
                                    String.class
                            );
                            if (switchStatus.isPresent() && switchStatus.get().equals("ON")) {
                                d.setIsOpen(true);
                                found = true; // 找到ON状态后立即跳出循环
                            }
                        }
                        // 如果没有找到ON状态，设置为false
                        if (!found) {
                            d.setIsOpen(false);
                        }
                    }
                }
                //8->灯光
                if (d.getDeviceTypeId() == 8) {
                    d.setIsOpenIcon(true);
                    //获取开关状态
                    Optional<Integer> count = DeviceInfo.getValueByName(d.getDeviceInfoList(), "count", Integer.class);
                    if (count.isPresent()) {
                        boolean found = false;
                        for (int i = 0; i < count.get() && !found; i++) {
                            Optional<String> switchStatus = DeviceInfo.getValueByName(
                                    d.getDeviceInfoList(),
                                    "switch" + (i + 1),
                                    String.class
                            );
                            if (switchStatus.isPresent() && switchStatus.get().equals("ON")) {
                                d.setIsOpen(true);
                                found = true; // 找到ON状态后立即跳出循环
                            }
                        }
                        // 如果没有找到ON状态，设置为false
                        if (!found) {
                            d.setIsOpen(false);
                        }
                    }
                }
            }
        });
        return PageResult.success(result);
    }

    @Operation(summary = "主从配置管理分页列表")
    @GetMapping("/master/page")
    public PageResult<DeviceMasterVO> getDeviceMasterPage(BasePageQuery queryParams) {
        // 1. 查询所有主设备
        IPage<Device> masterPage = deviceService.listAllMasterDevices(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()));
        if (ObjectUtils.isEmpty(masterPage.getRecords())) {
            return PageResult.success(new Page<>());
        }
        // 2. 获取房间内信息
        List<Long> roomIds = masterPage.getRecords().stream()
                .map(Device::getDeviceRoom)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Room> roomMap = roomService.listByIds(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, room -> room));
        // 3. 获取设备ID列表
        List<Long> deviceIds = masterPage.getRecords().stream()
                .map(Device::getId)
                .collect(Collectors.toList());
        // 4. 查询设备分类关系并过滤掉分类ID为null的记录
        List<CategoryDeviceRelationship> relationships = categoryDeviceRelationshipService.listByDeviceIds(deviceIds)
                .stream()
                .filter(r -> r.getCategoryId() != null)
                .toList();
        // 5. 构建设备到分类关系的映射
        Map<Long, Long> deviceToCategoryMap = relationships.stream()
                .collect(Collectors.toMap(
                        CategoryDeviceRelationship::getDeviceId,
                        CategoryDeviceRelationship::getCategoryId,
                        (existing, replacement) -> existing
                ));
        // 6. 获取所有分类ID并查询分类信息
        List<Long> categoryIds = relationships.stream()
                .map(CategoryDeviceRelationship::getCategoryId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Category> categoryMap = categoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, category -> category));
        // 7. 构建返回结果
        List<DeviceMasterVO> deviceMasterVOList = masterPage.getRecords().stream().map(masterDevice -> {
            DeviceMasterVO vo = new DeviceMasterVO();
            vo.setDeviceName(masterDevice.getDeviceName());
            // 设置房间内信息
            if (masterDevice.getDeviceRoom() != null) {
                Room room = roomMap.get(masterDevice.getDeviceRoom());
                if (room != null) {
                    vo.setRoomName(room.getClassroomCode());
                }
            }
            // 设置分类信息
            Long categoryId = deviceToCategoryMap.get(masterDevice.getId());
            if (categoryId != null) {
                Category category = categoryMap.get(categoryId);
                if (category != null) {
                    vo.setCategoryName(category.getCategoryName());
                } else {
                    vo.setCategoryName("未分类");
                }
            } else {
                // 没有分类关系，设置默认值
                vo.setCategoryName("未分类");
            }
            return vo;
        }).collect(Collectors.toList());
        IPage<DeviceMasterVO> resultPage = new Page<>(masterPage.getCurrent(), masterPage.getSize(), masterPage.getTotal());
        resultPage.setRecords(deviceMasterVOList);
        return PageResult.success(resultPage);
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
    @Log(value = "新增设备", module = LogModuleEnum.DEVICE)
    public Result<Void> saveDevice(@RequestBody @Valid DeviceForm formData) throws MqttException {
        //校验编码是否重复
        boolean isExistCode = deviceService.isExistDeviceNo(formData.getDeviceNo());
        if (isExistCode) {
            return Result.failed("设备编码已存在");
        }
        //校验MAC是否存在
        boolean isExistMac = deviceService.isExistDeviceMac(formData.getDeviceMac());
        if (isExistMac) {
            return Result.failed("设备已存在");
        }
        boolean result = false;
        //目前switch应该分ZigBee网关 ZigBee网关子设备 MQTT独立设备 WIFI独立设备
        //所以先根据通讯方式做不同处理
        switch (formData.getCommunicationModeItemId().intValue()) {
            case 1, 5:
                //ZigBee网关子设备
                zigBeeDevice(formData);
                result = deviceService.saveDevice(formData);
                break;
            case 2:
                //WIFI独立设备 tasmota
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
        if (result) {
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, formData.getDeviceCode(), deviceService.getById(formData.getId()));
            //处理category
            if (formData.getCategoryId() != null) {
                CategoryDeviceRelationship categoryDeviceRelationship = new CategoryDeviceRelationship();
                categoryDeviceRelationship.setCategoryId(formData.getCategoryId());
                categoryDeviceRelationship.setDeviceId(formData.getId());
                categoryDeviceRelationshipService.save(categoryDeviceRelationship);
            }
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
    @Log(value = "修改设备", module = LogModuleEnum.DEVICE)
    public Result<Void> updateDevice(
            @Parameter(description = "设备管理ID") @PathVariable Long id,
            @RequestBody @Validated DeviceForm formData
    ) {
        boolean result = deviceService.updateDevice(id, formData);
        //更新缓存
        redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, formData.getDeviceCode(), deviceService.getById(id));
        // 处理设备分类关系
        if (formData.getCategoryId() != null) {
            // 1. 查询设备当前的分类关系
            CategoryDeviceRelationship relationship = categoryDeviceRelationshipService.getOne(
                    new LambdaQueryWrapper<CategoryDeviceRelationship>()
                            .eq(CategoryDeviceRelationship::getDeviceId, id)
            );

            // 2. 如果已存在分类关系
            if (ObjectUtils.isNotEmpty(relationship)) {
                // 2.1 分类ID不同则更新
                if (!relationship.getCategoryId().equals(formData.getCategoryId())) {
                    categoryDeviceRelationshipService.removeById(relationship);
                    CategoryDeviceRelationship newRelationship = new CategoryDeviceRelationship();
                    newRelationship.setCategoryId(formData.getCategoryId());
                    newRelationship.setDeviceId(id);
                    categoryDeviceRelationshipService.save(newRelationship);
                }
                // 2.2 分类ID相同则不做处理
            }
            // 3. 不存在分类关系则新增
            else {
                CategoryDeviceRelationship newRelationship = new CategoryDeviceRelationship();
                newRelationship.setCategoryId(formData.getCategoryId());
                newRelationship.setDeviceId(id);
                categoryDeviceRelationshipService.save(newRelationship);
            }
        }
// 4. 取消绑定分类(分类ID为null)
        else {
            CategoryDeviceRelationship relationship = categoryDeviceRelationshipService.getOne(
                    new LambdaQueryWrapper<CategoryDeviceRelationship>()
                            .eq(CategoryDeviceRelationship::getDeviceId, id)
            );
            if (ObjectUtils.isNotEmpty(relationship)) {
                categoryDeviceRelationshipService.removeById(relationship);
            }
        }
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
    @Log(value = "删除设备", module = LogModuleEnum.DEVICE)
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
                    //MQTT独立设备
                    mqttDeviceDel(device);
                    result = deviceService.removeById(device.getId());
                    break;
                case 3:
                    //ZigBee网关
                    //有设备则无法删除
                    List<Device> subDevices = deviceService.listGatewaySubDevices(device.getId());
                    if (!subDevices.isEmpty()) {
                        return Result.failed("网关下有设备，无法删除");
                    }
                    zigBeeGateWayDelDel(device);
                    result = deviceService.removeById(device.getId());
                    break;
                default:
                    break;
            }
        }

        if (result) {
            //删除的同时清缓存
            redisTemplate.opsForHash().delete(RedisConstants.Device.DEVICE, idList);
            //删除关系表
            categoryDeviceRelationshipService.remove(new LambdaQueryWrapper<CategoryDeviceRelationship>().eq(CategoryDeviceRelationship::getDeviceId, idList));
        }
        return Result.judge(result);
    }

    @Operation(summary = "重新录入设备")
    @GetMapping("/reEnter/{id}")
    @Log(value = "重录入设备", module = LogModuleEnum.DEVICE)
    public Result<Void> reEnter(
            @Parameter(description = "设备ID") @PathVariable Long id) throws MqttException {
        Device device = deviceService.getById(id);
        //必须为zigBee子设备
        if (ObjectUtils.isEmpty(device)) return Result.failed("设备不存在");
        if (device.getCommunicationModeItemId() != 1 && device.getCommunicationModeItemId() != 5) {
            return Result.failed("设备不是zigBee子设备");
        }
        if (device.getDeviceGatewayId() == null) return Result.failed("设备没有网关");
        Device gateway = deviceService.getById(device.getDeviceGatewayId());
        if (ObjectUtils.isEmpty(gateway)) return Result.failed("该设备没有网关");
        //发mqtt
        gatewayDeviceAddMqtt(gateway.getDeviceCode());
        device.setStatus(0);
        deviceService.updateById(device);
        return Result.success();
    }


    /**
     * @description: 房间页详情里配置主从按钮的接口
     * @author: way
     * @date: 2025/7/15 15:15
     * @param: [ids, isMaster, roomId]
     * @return: com.youlai.boot.common.result.Result<java.lang.Void>
     **/
    @Operation(summary = "主从配置")
    @GetMapping("/masterSlave")
    @Log(value = "主从配置", module = LogModuleEnum.DEVICE)
    @PreAuthorize("@ss.hasPerm('device:device:master')")
    public Result<Void> masterSlave(
            @Parameter(description = "设备ID列表") @RequestParam String ids,
            @Parameter(description = "是否为主设备") @RequestParam Boolean isMaster,
            @Parameter(description = "房间Id") @RequestParam Long roomId
    ) {
        deviceService.masterSlave(ids, isMaster, roomId);
        return Result.success();
    }

    private void mqttDeviceDel(Device device) {
        //删缓存
        redisTemplate.opsForHash().delete(RedisConstants.Device.DEVICE, device.getDeviceCode());
        mqttCallback.unsubscribeTopic("tele/" + device.getDeviceCode() + "/SENSOR");
        log.info("动态取消订阅主题\"tele/\" + device.getDeviceCode() + \"/SENSOR\"");
        mqttCallback.unsubscribeTopic("tele/" + device.getDeviceCode() + "/INFO3");
        log.info("动态取消订阅主题\"tele/\" + device.getDeviceCode() + \"/INFO3\"");
        mqttCallback.unsubscribeTopic("tele/" + device.getDeviceCode() + "/STATE");
        log.info("动态取消订阅主题\"tele/\" + device.getDeviceCode() + \"/STATE\"");
    }

    private void zigBeeGateWayDelDel(Device device) {
        //删缓存
        redisTemplate.opsForHash().delete(RedisConstants.Device.DEVICE, device.getDeviceCode());
        //实时取消订阅
        String deviceMac = MacUtils.reParseMACAddress(device.getDeviceMac());
        for (String consumerTopic : TOPIC_LIST) {
            mqttCallback.unsubscribeTopic(BASE_TOPIC + deviceMac + consumerTopic);
            log.info("动态取消订阅主题: {}", BASE_TOPIC + deviceMac + consumerTopic);
        }
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
        if (ObjectUtils.isEmpty(gateway)) return;
        //给网关发送删除子设备消息
        HashMap<String, Object> rootMap = sendDelToGatewayData(device);
        mqttProducer.send("/zbgw/" + gateway.getDeviceCode() + "/manage", 2, false, JSON.toJSONString(rootMap));
    }

    @NotNull
    private static HashMap<String, Object> sendDelToGatewayData(Device device) {
        //1.构造消息
        // 构建最外层HashMap
        HashMap<String, Object> rootMap = new HashMap<>();
        // 设置sequence字段（Integer类型）
        rootMap.put("sequence", (int) System.currentTimeMillis());
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
        return rootMap;
    }

    private void wifiDevice(@Valid DeviceForm formData) {
        mqttCallback.subscribeTopic("tele/" + formData.getDeviceMac() + "/SENSOR");
        mqttCallback.subscribeTopic("tele/" + formData.getDeviceMac() + "/INFO3");
        mqttCallback.subscribeTopic("tele/" + formData.getDeviceMac() + "/STATE");
//        mqttCallback.subscribeTopic("stat/" + formData.getDeviceMac() + "/POWER");
        mqttCallback.subscribeTopic("stat/" + formData.getDeviceMac() + "/RESULT");
        mqttCallback.subscribeTopic("stat/" + formData.getDeviceCode() + "/STATUS8");
    }

    private void mqttDevice(@Valid DeviceForm formData) {
        log.info("{}", formData);
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

    @Operation(summary = "网关扫描子设备动作")
    @GetMapping("/scan")
    public void scan(@RequestParam String gateway) throws MqttException {
        ///zbgw/9454c5ee8180/sub/manage
        gatewayDeviceAddMqtt(gateway);
    }

    public void gatewayDeviceAddMqtt(String macTopic) throws MqttException {
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
