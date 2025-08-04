package com.youlai.boot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;
import com.youlai.boot.categoryDeviceRelationship.service.CategoryDeviceRelationshipService;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.core.security.util.SecurityUtils;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.Enum.DeviceTypeEnum;
import com.youlai.boot.device.converter.DeviceConverter;
import com.youlai.boot.device.factory.DeviceInfoParserFactory;
import com.youlai.boot.device.mapper.DeviceMapper;
import com.youlai.boot.device.model.dto.event.DeviceEventParams;
import com.youlai.boot.device.model.dto.event.SubDevicesEvent;
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
    private final CategoryDeviceRelationshipService categoryDeviceRelationshipService;
    private final CategoryService categoryService;

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
        return this.baseMapper.getDevicePage(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), queryParams);
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
        DeviceForm form = deviceConverter.toForm(entity);
        //查询分类
        CategoryDeviceRelationship relationship = categoryDeviceRelationshipService.getByDeviceId(id);
        if (ObjectUtils.isNotEmpty(relationship)) {
            form.setCategoryId(relationship.getCategoryId());
            //查询分类名称
            Category category = categoryService.getById(relationship.getCategoryId());
            if (ObjectUtils.isNotEmpty(category)) {
                form.setCategoryName(category.getCategoryName());
            }
        }
        return form;
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
        entity.setCreateBy(SecurityUtils.getUserId());
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
        entity.setUpdateBy(SecurityUtils.getUserId());
        return this.updateById(entity);
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
        List<SubDevicesEvent> subDevices = params.getSubDevices();
        for (SubDevicesEvent subDevice : subDevices) {
            Device deviceUpdate = new Device();
            if (subDevice.getOnline() != null) {
                if (subDevice.getOnline()) {
                    //在线
                    deviceUpdate.setStatus(1);
                } else {
                    //离线
                    deviceUpdate.setStatus(0);
                }
                this.deviceMapper.update(deviceUpdate, new LambdaQueryWrapper<Device>().eq(Device::getDeviceCode, subDevice.getDeviceId()));
            }
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

    @Override
    public IPage<DeviceVO> getSubDevicePage(DeviceQuery queryParams) {
        return this.deviceMapper.getSubDevicePage(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), queryParams);
    }

    @Override
    public List<DeviceInfoVO> listDeviceByRoomId(Long roomId, Room room) {
        List<Device> roomDevices = this.list(new LambdaQueryWrapper<Device>().eq(Device::getDeviceRoom, roomId).eq(Device::getStatus, 1));
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
            List<Device> floorDevices = this.list(new LambdaQueryWrapper<Device>().in(Device::getDeviceRoom, roomList.stream().map(Room::getId).toList()).eq(Device::getStatus, 1));
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
        List<Device> roomDevices = this.list(new LambdaQueryWrapper<Device>().in(Device::getDeviceRoom, records.stream().map(RoomVO::getId).toArray()).eq(Device::getStatus, 1));
        Map<Object, Object> device = redisTemplate.opsForHash().entries(RedisConstants.Device.DEVICE);
        Map<String, Device> resultMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : device.entrySet()) {
            // 假设key可以安全转换为String
            String deviceCode = entry.getKey().toString();
            // 假设value就是Device对象，直接强制类型转换
            Device deviceObj = (Device) entry.getValue();
            resultMap.put(deviceCode, deviceObj);
        }
        roomDevices
                .forEach(item -> {
                    Device deviceInfo = resultMap.get(item.getDeviceCode());
                    if (deviceInfo != null) {
                        item.setDeviceInfo(deviceInfo.getDeviceInfo());
                    }
                });
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
            List<Device> floorDevices = this.list(new LambdaQueryWrapper<Device>().in(Device::getDeviceRoom, roomList.stream().map(Room::getId).toList()).eq(Device::getStatus, 1));
            return devicesToInfoVos(floorDevices);
        }
        return null;
    }

    @Override
    public Map<String, Long> countDevicesByStatus() {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        wrapper.select("status, count(*) as count").groupBy("status");

        List<Map<String, Object>> list = deviceMapper.selectMaps(wrapper);

        Map<String, Long> result = new HashMap<>();
        result.put("disable", 0L);
        result.put("online", 0L);
        result.put("unregistered", 0L);
        result.put("offline", 0L);
        for (Map<String, Object> map : list) {
            Integer status = (Integer) map.get("status");
            Long count = (Long) map.get("count");

            switch (status) {
                case 3:
                    result.put("disable", count);
                    break;
                case 1:
                    result.put("online", count);
                    break;
                case 2:
                    result.put("unregistered", count);
                    break;
                case 0:
                    result.put("offline", count);
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

        List<Long> idList = Arrays.stream(ids.split(",")).map(Long::parseLong).collect(Collectors.toList());

        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>().eq(Device::getIsDeleted, 0);

        switch (type) {
            case "building":
                // 查询建筑下的设备数量（通过房间关联）
                List<Room> rooms = roomService.list(new LambdaQueryWrapper<Room>().in(Room::getBuildingId, idList).eq(Room::getIsDeleted, 0));
                if (ObjectUtils.isEmpty(rooms)) {
                    return 0L;
                }
                queryWrapper.in(Device::getDeviceRoom, rooms.stream().map(Room::getId).collect(Collectors.toList()));
                break;

            case "floor":
                // 查询楼层下的设备数量（通过房间关联）
                List<Room> floorRooms = roomService.list(new LambdaQueryWrapper<Room>().in(Room::getFloorId, idList).eq(Room::getIsDeleted, 0));
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

    @Override
    public boolean isExistDeviceNo(String deviceNo) {
        return deviceMapper.selectCount(new LambdaQueryWrapper<Device>().eq(Device::getDeviceNo, deviceNo)) > 0;
    }

    @Override
    public List<Device> getGateway() {
        return deviceMapper.selectList(new LambdaQueryWrapper<Device>().eq(Device::getDeviceTypeId, 1));
    }

    @Override
    public List<Device> listGatewaySubDevices(Long gatewayId) {
        return deviceMapper.selectList(new LambdaQueryWrapper<Device>().eq(Device::getDeviceGatewayId, gatewayId));
    }

    @Override
    public List<Device> listMqttDevices() {
        return deviceMapper.selectList(new LambdaQueryWrapper<Device>().eq(Device::getCommunicationModeItemId, 4));
    }

    @Override
    public void masterSlave(String ids, Boolean isMaster, Long roomId) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        List<Device> devices = this.listByIdAndRoomId(idList, roomId);
        for (Device device : devices) {
            device.setIsMaster(isMaster ? 1 : 0);
            //缓存同步
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
        }
        this.updateBatchById(devices);

    }

    @Override
    public IPage<Device> listAllMasterDevices(Page<Device> page) {
        return this.page(page,
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getIsMaster, 1)
        );
    }

    @Override
    public void masterSlaveDel(String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        List<Device> devices = this.listByIds(idList);
        for (Device device : devices) {
            device.setIsMaster(0);
            //缓存同步
            redisTemplate.opsForHash().put(RedisConstants.Device.DEVICE, device.getDeviceCode(), device);
        }
        this.updateBatchById(devices);
    }

    @Override
    public List<String> listMetric(Long deviceId) {
        Device device = this.getById(deviceId);
        if (device == null) {
            return null;
        }
        if (device.getDeviceInfo() == null) {
            return null;
        }
        JsonNode deviceInfo = device.getDeviceInfo();
        //把软属性key列成列表
        List<String> metricList = new ArrayList<>();
        // 遍历设备信息中的所有字段名
        Iterator<String> fieldNames = deviceInfo.fieldNames();
        while (fieldNames.hasNext()) {
            metricList.add(fieldNames.next());
        }
        return metricList;

    }

    @Override
    public List<Option<Long>> listDeviceOptions() {
        List<Device> list = this.list(new LambdaQueryWrapper<Device>().eq(Device::getStatus, 1));
        return deviceConverter.toOptions(list);
    }

    @Override
    public List<Option<Long>> listMetricsOption(String ids) {
        List<Device> devices = this.listByIds(Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList());
        if (devices == null) {
            return null;
        }
        List<Option<Long>> options = new ArrayList<>();
        for (Device device : devices) {
            if (device.getDeviceInfo() != null) {
                Option<Long> option = new Option<>();
                option.setValue(device.getId());
                JsonNode deviceInfo = device.getDeviceInfo();
                //把软属性key列成列表
                List<String> metricList = new ArrayList<>();
                // 遍历设备信息中的所有字段名
                Iterator<String> fieldNames = deviceInfo.fieldNames();
                while (fieldNames.hasNext()) {
                    metricList.add(fieldNames.next());
                }
                //将metricList所有字段名称以逗号分隔开拼成一个字符串
                option.setLabel(String.join(",", metricList));
                options.add(option);
            }
        }
        return options;
    }

    private List<Device> listByIdAndRoomId(List<Long> idList, Long roomId) {
        if (idList.size() == 1) {
            if (idList.get(0) == -1) {
                return this.list(new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceRoom, roomId));
            }
        }
        return this.list(new QueryWrapper<Device>().lambda()
                .eq(Device::getDeviceRoom, roomId).in(Device::getId, idList)
                .in(Device::getId, idList)
        );
    }
}
