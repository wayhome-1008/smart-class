package com.youlai.boot.device.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.device.model.dto.event.DeviceEventParams;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.form.DeviceForm;
import com.youlai.boot.device.model.query.DeviceQuery;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.model.vo.DeviceVO;
import com.youlai.boot.floor.model.entity.Floor;
import com.youlai.boot.floor.model.vo.FloorVO;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.model.vo.RoomVO;

import java.util.List;
import java.util.Map;

/**
 * 设备管理服务类
 *
 * @author way
 * @since 2025-05-08 15:16
 */
public interface DeviceService extends IService<Device> {

    /**
     *设备管理分页列表
     *
     * @return {@link IPage<DeviceVO>} 设备管理分页列表
     */
    IPage<DeviceVO> getDevicePage(DeviceQuery queryParams);

    /**
     * 获取设备管理表单数据
     *
     * @param id 设备管理ID
     * @return 设备管理表单数据
     */
     DeviceForm getDeviceFormData(Long id);

    /**
     * 新增设备管理
     *
     * @param formData 设备管理表单对象
     * @return 是否新增成功
     */
    boolean saveDevice(DeviceForm formData);

    /**
     * 修改设备管理
     *
     * @param id   设备管理ID
     * @param formData 设备管理表单对象
     * @return 是否修改成功
     */
    boolean updateDevice(Long id, DeviceForm formData);

//    /**
//     * 删除设备管理
//     *
//     * @param ids 设备管理ID，多个以英文逗号(,)分割
//     * @return 是否删除成功
//     */
//    boolean deleteDevices(String ids);

    List<Device> getDeviceList();

    boolean isExistDeviceMac(String deviceMac);

    Device getByMac(String macAddress);

    void updateDeviceStatusByCode(DeviceEventParams params);

    Device getByCode(String code);

    List<Option<Long>> listGatewayOptions();

    IPage<DeviceVO> getSubDevicePage( DeviceQuery queryParams);

    List<DeviceInfoVO> listDeviceByRoomId(Long roomId, Room room);

    List<DeviceInfoVO> listDeviceByFloorId(Long floorId, Floor floor);

    List<DeviceInfoVO> listDeviceByRoomIds(List<RoomVO> records);

    List<Long> listRoomDevicesIds(Long id);

    List<DeviceInfoVO> listDeviceByFloorIds(List<FloorVO> records);

    Map<String, Long> countDevicesByStatus();

    Long  listDevicesCount(String type,String ids);

    boolean isExistDeviceNo(String deviceNo);

    List<Device> getGateway();

    List<Device> listGatewaySubDevices(Long gatewayId);

    List<Device> listMqttDevices();

    void masterSlave(String ids, Boolean isMaster, Long roomId);

//    Boolean masterSlave(@Valid MasterSlaveForm formData);
//
//    List<Option<Long>> listDeviceMasterSlaveOptions();

}
