package com.youlai.boot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.config.mqtt.MqttProducer;
import com.youlai.boot.device.model.dto.event.DeviceEventParams;
import com.youlai.boot.device.model.dto.event.SubDevicesEvent;
import com.youlai.boot.deviceType.mapper.DeviceTypeMapper;
import com.youlai.boot.deviceType.model.entity.DeviceType;
import com.youlai.boot.system.mapper.DictItemMapper;
import com.youlai.boot.system.mapper.DictMapper;
import com.youlai.boot.system.model.entity.Dict;
import com.youlai.boot.system.model.entity.DictItem;
import com.youlai.boot.system.service.DictItemService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.device.mapper.DeviceMapper;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceForm;
import com.youlai.boot.device.model.query.DeviceQuery;
import com.youlai.boot.device.model.vo.DeviceVO;
import com.youlai.boot.device.converter.DeviceConverter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 设备管理服务实现类
 *
 * @author way
 * @since 2025-05-08 15:16
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {
    private final DeviceMapper deviceMapper;
    private final DeviceConverter deviceConverter;
    private final DictItemService dictItemService;
    private final DeviceTypeMapper deviceTypeMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void init() {
        //初始化数据库设备列表到redis
        refreshCache();
    }

    private void refreshCache() {
        redisTemplate.delete(RedisConstants.Device.DEVICE);
        List<Device> list = this.list();
        if (list != null) {
            Map<String, Device> map = list.stream().collect(Collectors.toMap(Device::getDeviceCode, device -> device));
            redisTemplate.opsForHash().putAll(RedisConstants.Device.DEVICE, map);
        }
    }

    /**
     * 获取设备管理分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<DeviceVO>} 设备管理分页列表
     */
    @Override
    public IPage<DeviceVO> getDevicePage(DeviceQuery queryParams) {
        Page<DeviceVO> pageVO = this.baseMapper.getDevicePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }

    /**
     * 获取设备管理表单数据
     *
     * @param id 设备管理ID
     * @return 设备管理表单数据
     */
    @Override
    public DeviceForm getDeviceFormData(Long id) {
        Device entity = this.getById(id);
        //查询设备类型
        DeviceType deviceType = deviceTypeMapper.selectById(entity.getDeviceTypeId());
        entity.setDeviceType(deviceType.getDeviceType());
        //同时查询设备类型名称和通讯方式名称
        List<DictItem> dictEntry = dictItemService.listByDictCode("communication_mode");
        for (DictItem dictItem : dictEntry) {
            if (NumberUtils.toLong(dictItem.getValue()) == entity.getCommunicationModeItemId()) {
                entity.setCommunicationModeItemName(dictItem.getLabel());
            }
        }
        return deviceConverter.toForm(entity);
    }

    /**
     * 新增设备管理
     *
     * @param formData 设备管理表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveDevice(DeviceForm formData) {
        Device entity = deviceConverter.toEntity(formData);
        //新增设备默认状态非正常 需要handler主动修改
        entity.setStatus(0);
        return this.save(entity);
    }

    /**
     * 更新设备管理
     *
     * @param id   设备管理ID
     * @param formData 设备管理表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateDevice(Long id, DeviceForm formData) {
        Device entity = deviceConverter.toEntity(formData);
        return this.updateById(entity);
    }

    /**
     * 删除设备管理
     *
     * @param ids 设备管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteDevices(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的设备管理数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    @Override
    public List<Device> getDeviceList() {
        return this.deviceMapper.selectList(new LambdaQueryWrapper<Device>().eq(Device::getIsDeleted, 0));
    }

    @Override
    public boolean isExistDeviceMac(String deviceMac) {
        return deviceMapper.selectCount(new LambdaQueryWrapper<Device>().eq(Device::getDeviceMac, deviceMac)) > 0;
    }

    @Override
    public Device getByMac(String macAddress) {
        return this.deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getDeviceMac, macAddress).eq(Device::getIsDeleted, 0));
    }

    @Override
    public void updateDeviceStatusByCode(DeviceEventParams params) {
        List<SubDevicesEvent> subDevices = params.getSubDevices();
        for (SubDevicesEvent subDevice : subDevices) {
            Device deviceUpdate = new Device();
            deviceUpdate.setStatus(subDevice.getOnline() ? 1 : 0);
            this.deviceMapper.update(deviceUpdate, new LambdaQueryWrapper<Device>().eq(Device::getDeviceCode, subDevice.getDeviceId()));
        }
    }

    @Override
    public Device getByCode(String code) {
        return this.deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getDeviceCode, code));
    }

    @Override
    public List<Option<Long>> listGatewayOptions() {
        List<Device> list = this.list(new LambdaQueryWrapper<Device>().eq(Device::getStatus, 1).eq(Device::getDeviceTypeId, 1));
        return deviceConverter.toOptions(list);
    }

}
