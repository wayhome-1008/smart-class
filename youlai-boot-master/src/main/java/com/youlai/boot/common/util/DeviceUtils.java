package com.youlai.boot.common.util;

import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.Enum.DeviceTypeEnum;
import com.youlai.boot.device.factory.DeviceInfoParserFactory;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.service.DeviceInfoParser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.youlai.boot.dashBoard.controller.DashBoardController.basicPropertyConvert;

/**
 *@Author: way
 *@CreateTime: 2025-06-13  14:09
 *@Description: TODO
 */
public class DeviceUtils {
    @NotNull
    public static List<DeviceInfoVO> devicesToInfoVos(List<Device> roomDevices) {
        List<DeviceInfoVO> deviceInfoVOS = new ArrayList<>();
        for (Device roomDevice : roomDevices) {
            //转VO
            DeviceInfoVO deviceInfoVO = basicPropertyConvert(roomDevice, roomDevice.getRoomName());
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
    }
}
