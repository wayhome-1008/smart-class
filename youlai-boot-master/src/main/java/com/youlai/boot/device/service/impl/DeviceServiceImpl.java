package com.youlai.boot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.Enum.DeviceTypeEnum;
import com.youlai.boot.device.converter.DeviceConverter;
import com.youlai.boot.device.factory.DeviceInfoParserFactory;
import com.youlai.boot.device.mapper.DeviceMapper;
import com.youlai.boot.device.model.dto.event.DeviceEventParams;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceForm;
import com.youlai.boot.device.model.query.DeviceQuery;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.model.vo.DeviceVO;
import com.youlai.boot.device.service.DeviceInfoParser;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.deviceType.mapper.DeviceTypeMapper;
import com.youlai.boot.deviceType.model.entity.DeviceType;
import com.youlai.boot.floor.model.entity.Floor;
import com.youlai.boot.floor.model.vo.FloorVO;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.model.vo.RoomVO;
import com.youlai.boot.room.service.RoomService;
import com.youlai.boot.system.model.entity.DictItem;
import com.youlai.boot.system.service.DictItemService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.youlai.boot.common.util.DeviceUtils.devicesToInfoVos;
import static com.youlai.boot.dashBoard.controller.DashBoardController.basicPropertyConvert;

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
    private final RoomService roomService;

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
        return this.baseMapper.getDevicePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
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
        entity.setStatus(2);
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

//    /**
//     * 删除设备管理
//     *
//     * @param ids 设备管理ID，多个以英文逗号(,)分割
//     * @return 是否删除成功
//     */
//    @Override
//    public boolean deleteDevices(String ids) {
//        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的设备管理数据为空");
//        // 逻辑删除
//        List<Long> idList = Arrays.stream(ids.split(","))
//                .map(Long::parseLong)
//                .toList();
//        return this.removeByIds(idList);
//    }

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
        //todo 状态处理
//        List<SubDevicesEvent> subDevices = params.getSubDevices();
//        for (SubDevicesEvent subDevice : subDevices) {
//            Device deviceUpdate = new Device();
//            if (subDevice.getOnline() != null) {
//                deviceUpdate.setStatus(subDevice.getOnline() ? 1 : 0);
//                this.deviceMapper.update(deviceUpdate, new LambdaQueryWrapper<Device>().eq(Device::getDeviceCode, subDevice.getDeviceId()));
//            }
//        }
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

    @Override
    public IPage<DeviceVO> getSubDevicePage(DeviceQuery queryParams) {
        return this.deviceMapper.getSubDevicePage(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), queryParams);
    }

    @Override
    public List<DeviceInfoVO> listDeviceByRoomId(Long roomId, Room room) {
        List<Device> roomDevices = this.list(new LambdaQueryWrapper<Device>().eq(Device::getDeviceRoom, roomId));
        if (ObjectUtils.isNotEmpty(roomDevices)) {
            List<DeviceInfoVO> deviceInfoVOS = new ArrayList<>();
            for (Device roomDevice : roomDevices) {
                //转VO
                DeviceInfoVO deviceInfoVO = basicPropertyConvert(roomDevice, room.getClassroomCode());
                String deviceType = DeviceTypeEnum.getNameById(roomDevice.getDeviceTypeId());
                String communicationMode = CommunicationModeEnum.getNameById(roomDevice.getCommunicationModeItemId());
                if (!deviceType.equals("Gateway")) {
                    DeviceInfoParser parser = DeviceInfoParserFactory.getParser(deviceType, communicationMode);
                    List<DeviceInfo> deviceInfos = parser.parse(roomDevice.getDeviceInfo());
                    deviceInfoVO.setDeviceInfo(deviceInfos);
                }
                deviceInfoVOS.add(deviceInfoVO);
            }
            return deviceInfoVOS;
        } else {
            return null;
        }
    }

    @Override
    public List<DeviceInfoVO> listDeviceByFloorId(Long floorId, Floor floor) {
        //1.查询该楼层有那些roomId
        List<Room> roomList = roomService.list(new LambdaQueryWrapper<Room>().eq(Room::getFloorId, floorId));
        if (ObjectUtils.isNotEmpty(roomList)) {
            List<Device> floorDevices = this.list(new LambdaQueryWrapper<Device>().in(Device::getDeviceRoom, roomList.stream().map(Room::getId).toList()));
            List<DeviceInfoVO> deviceInfoVOS = new ArrayList<>();
            for (Device floorDevice : floorDevices) {
                for (Room room : roomList) {
                    if (Objects.equals(room.getId(), floorDevice.getDeviceRoom())) {
                        //转VO
                        DeviceInfoVO deviceInfoVO = basicPropertyConvert(floorDevice, room.getClassroomCode());
                        String deviceType = DeviceTypeEnum.getNameById(floorDevice.getDeviceTypeId());
                        String communicationMode = CommunicationModeEnum.getNameById(floorDevice.getCommunicationModeItemId());
                        if (!deviceType.equals("Gateway")) {
                            DeviceInfoParser parser = DeviceInfoParserFactory.getParser(deviceType, communicationMode);
                            List<DeviceInfo> deviceInfos = parser.parse(floorDevice.getDeviceInfo());
                            deviceInfoVO.setDeviceInfo(deviceInfos);
                        }
                        deviceInfoVOS.add(deviceInfoVO);
                    }
                }
            }
            return deviceInfoVOS;
        } else {
            return null;
        }
    }

    @Override
    public List<DeviceInfoVO> listDeviceByRoomIds(List<RoomVO> records) {
        List<Device> roomDevices = this.list(new LambdaQueryWrapper<Device>().in(Device::getDeviceRoom, records.stream().map(RoomVO::getId).toArray()));
        return devicesToInfoVos(roomDevices);
    }


    @Override
    public List<Long> listRoomDevicesIds(Long id) {
        return this.list(new LambdaQueryWrapper<Device>().eq(Device::getDeviceRoom, id)).stream().map(Device::getDeviceTypeId).toList();
    }

    @Override
    public List<DeviceInfoVO> listDeviceByFloorIds(List<FloorVO> records) {
        List<Room> roomList = roomService.list(new LambdaQueryWrapper<Room>().in(Room::getFloorId, records.stream().map(FloorVO::getId).toArray()));
        if (ObjectUtils.isNotEmpty(roomList)) {
            List<Device> floorDevices = this.list(new LambdaQueryWrapper<Device>().in(Device::getDeviceRoom, roomList.stream().map(Room::getId).toList()));
            return devicesToInfoVos(floorDevices);
        }
        return null;
    }

    @Override
    public Map<String, Long> countDevicesByStatus() {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        wrapper.select("status, count(*) as count")
                .groupBy("status");

        List<Map<String, Object>> list = deviceMapper.selectMaps(wrapper);

        Map<String, Long> result = new HashMap<>();
        result.put("online", 0L);
        result.put("offline", 0L);
        result.put("unregistered", 0L);

        for (Map<String, Object> map : list) {
            Integer status = (Integer) map.get("status");
            Long count = (Long) map.get("count");

            switch (status) {
                case 0:
                    result.put("online", count);
                    break;
                case 1:
                    result.put("offline", count);
                    break;
                case 2:
                    result.put("unregistered", count);
                    break;
            }
        }
        return result;
    }

    @Override
    public Long listDevicesCount(String type, String ids) {
        if (StringUtils.isBlank(ids)) {
            return 0L;
        }

        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getIsDeleted, 0);

        switch (type) {
            case "building":
                // 查询建筑下的设备数量（通过房间关联）
                List<Room> rooms = roomService.list(
                        new LambdaQueryWrapper<Room>()
                                .in(Room::getBuildingId, idList)
                                .eq(Room::getIsDeleted, 0)
                );
                if (ObjectUtils.isEmpty(rooms)) {
                    return 0L;
                }
                queryWrapper.in(Device::getDeviceRoom, rooms.stream().map(Room::getId).collect(Collectors.toList()));
                break;

            case "floor":
                // 查询楼层下的设备数量（通过房间关联）
                List<Room> floorRooms = roomService.list(
                        new LambdaQueryWrapper<Room>()
                                .in(Room::getFloorId, idList)
                                .eq(Room::getIsDeleted, 0)
                );
                if (ObjectUtils.isEmpty(floorRooms)) {
                    return 0L;
                }
                queryWrapper.in(Device::getDeviceRoom, floorRooms.stream().map(Room::getId).collect(Collectors.toList()));
                break;

            case "room":
                // 直接查询房间下的设备数量
                queryWrapper.in(Device::getDeviceRoom, idList);
                break;

            default:
                return 0L;
        }

        return this.count(queryWrapper);
    }
}
