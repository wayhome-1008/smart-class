package com.youlai.boot.dashBoard.controller;


import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.InfluxQueryBuilder;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.dashBoard.model.vo.DashCount;
import com.youlai.boot.device.Enum.CommunicationModeEnum;
import com.youlai.boot.device.Enum.DeviceTypeEnum;
import com.youlai.boot.device.factory.DeviceInfoParserFactory;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.influx.InfluxPlug;
import com.youlai.boot.device.model.influx.InfluxSensor;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.model.vo.DeviceVO;
import com.youlai.boot.device.service.DeviceInfoParser;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.service.RoomService;
import com.youlai.boot.system.service.LogService;
import com.youlai.boot.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *@Author: way
 *@CreateTime: 2025-05-22  16:59
 *@Description: 首页看板数据接口
 */
@Tag(name = "03.首页看板接口")
@RestController
@RequestMapping("/api/v1/dashBoard")
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DashBoardController {
    private final DeviceService deviceService;
    private final UserService userService;
    private final LogService logService;
    private final RoomService roomService;
    private final InfluxDBProperties influxDBProperties;
    private final InfluxDBClient influxDBClient;

    @Operation(summary = "获取首页count数量")
    @GetMapping("/count")
    public Result<DashCount> getDashCount() {
        DashCount dashCount = new DashCount();
        dashCount.setDeviceCount(deviceService.count());
        dashCount.setUserCount(userService.count());
        dashCount.setLogCount(logService.count());
        dashCount.setRoomCount(roomService.count());
        dashCount.setDemo1Count(9344L);
        dashCount.setDemo2Count(10086L);
        return Result.success(dashCount);
    }

    @Operation(summary = "获取设备状态统计")
    @GetMapping("/status/count")
    public Result<Map<String, Long>> getDeviceStatusCount() {
        Map<String, Long> statusCounts = deviceService.countDevicesByStatus();
        return Result.success(statusCounts);
    }

    @Operation(summary = "根据设备code获取数据")
    @GetMapping("/{code}/info")
    public Result<DeviceInfoVO> getDeviceInfo(@PathVariable String code) {
        //获取实体对基本类型转换VO
        Device device = deviceService.getByCode(code);
        //根据roomId查询
        Room room = roomService.getById(device.getDeviceRoom());
        DeviceInfoVO deviceInfoVO = basicPropertyConvert(device, room.getClassroomCode());
        //使用工厂对设备具体信息转换
        // 动态获取解析器
        // 使用枚举获取类型名称
        String deviceType = DeviceTypeEnum.getNameById(device.getDeviceTypeId());
        String communicationMode = CommunicationModeEnum.getNameById(device.getCommunicationModeItemId());
        DeviceInfoParser parser = DeviceInfoParserFactory.getParser(deviceType, communicationMode);
        List<DeviceInfo> deviceInfos = parser.parse(device.getDeviceInfo());
        deviceInfoVO.setDeviceInfo(deviceInfos);
        if (ObjectUtils.isNotEmpty(device)) return Result.success(deviceInfoVO);
        return Result.failed();
    }

    @Operation(summary = "查询传感器数据")
    @GetMapping("/sensor/data")
    public Result<List<InfluxSensor>> getSensorData(@Parameter(description = "设备编码", required = true)
                                                    @RequestParam String deviceCode,

                                                    @Parameter(description = "时间范围值", example = "1")
                                                    @RequestParam Long timeAmount,

                                                    @Parameter(description = "时间单位（y-年/M-月/d-日/h-小时）", example = "d")
                                                    @RequestParam(defaultValue = "h") String timeUnit

//                                                    @Parameter(description = "统计类型（raw-原始数据/max-最大值/min-最小值/avg-平均值）", example = "raw")
//                                                    @RequestParam(defaultValue = "raw") String statsType
    ) {
        try {
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .last(timeAmount, timeUnit)
                    .measurement("device")
                    .deviceCode(deviceCode)
                    .addFilter("r._field == \"temperature\" or r._field == \"humidity\" or r._field == \"illuminance\" or r._field == \"battery\"");
            log.info("influxdb查询传感器语句{}", builder.pivot().build());
            List<InfluxSensor> tables = influxDBClient.getQueryApi().query(builder.pivot().build(), influxDBProperties.getOrg(), InfluxSensor.class);
            return Result.success(tables);
        } catch (InfluxException e) {
            System.err.println("error：" + e.getMessage());
        }
//            // 构建Flux查询
//            String fluxQuery = InfluxQueryBuilder.newBuilder()
//                    .bucket(influxDBProperties.getBucket())
//                    .timeRange(timeAmount, timeUnit)  // 通用时间范围设置
//                    .measurement("device")
//                    .deviceCode(deviceCode)
//                    .fields("temperature,humidity,illuminance,battery")  // 字段过滤（支持逗号分隔）
////                    .statsType(statsType)  // 设置统计类型
//                    .pivot()
//                    .build();
//            log.info("InfluxDB查询语句: {}", fluxQuery);
//            List<InfluxSensor> rawData = influxDBClient.getQueryApi()
//                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxSensor.class);
//            return Result.success(rawData);
//        } catch (InfluxException e) {
//            log.error("InfluxDB查询失败: {}", e.getMessage());
        return Result.failed();
//        }

    }

    @Operation(summary = "查询计量插座数据")
    @GetMapping("/plug/data")
    public Result<List<InfluxPlug>> getPlugData(@Parameter(description = "设备编码", required = true)
                                                @RequestParam String deviceCode,

                                                @Parameter(description = "时间范围值", example = "1")
                                                @RequestParam Long timeAmount,

                                                @Parameter(description = "时间单位（y-年/M-月/d-日/h-小时）", example = "d")
                                                @RequestParam(defaultValue = "h") String timeUnit

//                                                    @Parameter(description = "统计类型（raw-原始数据/max-最大值/min-最小值/avg-平均值）", example = "raw")
//                                                    @RequestParam(defaultValue = "raw") String statsType
    ) {
        try {
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .last(timeAmount, timeUnit)
                    .measurement("device")
                    .deviceCode(deviceCode)
                    .sort()
                    .addFilter("r._field == \"activePowerA\" or r._field == \"RMS_VoltageA\" or r._field==\"RMS_CurrentA\" or r._field==\"electricalEnergy\"");
            log.info("influxdb查询计量插座语句{}", builder.pivot().build());
            List<InfluxPlug> tables = influxDBClient.getQueryApi().query(builder.pivot().build(), influxDBProperties.getOrg(), InfluxPlug.class);
            return Result.success(tables);
        } catch (InfluxException e) {
            System.err.println("error：" + e.getMessage());
        }
//            // 构建Flux查询
//            String fluxQuery = InfluxQueryBuilder.newBuilder()
//                    .bucket(influxDBProperties.getBucket())
//                    .timeRange(timeAmount, timeUnit)  // 通用时间范围设置
//                    .measurement("device")
//                    .deviceCode(deviceCode)
//                    .fields("temperature,humidity,illuminance,battery")  // 字段过滤（支持逗号分隔）
////                    .statsType(statsType)  // 设置统计类型
//                    .pivot()
//                    .build();
//            log.info("InfluxDB查询语句: {}", fluxQuery);
//            List<InfluxSensor> rawData = influxDBClient.getQueryApi()
//                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxSensor.class);
//            return Result.success(rawData);
//        } catch (InfluxException e) {
//            log.error("InfluxDB查询失败: {}", e.getMessage());
        return Result.failed();
//        }

    }

    public static DeviceInfoVO basicPropertyConvert(Device device, String roomCode) {
        DeviceInfoVO deviceInfoVO = new DeviceInfoVO();
        deviceInfoVO.setId(device.getId());
        deviceInfoVO.setDeviceName(device.getDeviceName());
        deviceInfoVO.setDeviceCode(device.getDeviceCode());
        deviceInfoVO.setDeviceRoom(device.getDeviceRoom());
        deviceInfoVO.setRoomName(roomCode);
        deviceInfoVO.setDeviceMac(device.getDeviceMac());
        deviceInfoVO.setDeviceGatewayId(device.getDeviceGatewayId());
        deviceInfoVO.setDeviceTypeId(device.getDeviceTypeId());
        deviceInfoVO.setCommunicationModeItemId(device.getCommunicationModeItemId());
        deviceInfoVO.setDeviceNo(device.getDeviceNo());
        deviceInfoVO.setStatus(device.getStatus());
        deviceInfoVO.setRemark(device.getRemark());
        return deviceInfoVO;
    }

    public static DeviceInfoVO basicPropertyConvert(DeviceVO device, String roomCode) {
        DeviceInfoVO deviceInfoVO = new DeviceInfoVO();
        deviceInfoVO.setId(device.getId());
        deviceInfoVO.setDeviceName(device.getDeviceName());
        deviceInfoVO.setDeviceCode(device.getDeviceCode());
        deviceInfoVO.setDeviceRoom(device.getDeviceRoom());
        deviceInfoVO.setRoomName(roomCode);
        deviceInfoVO.setDeviceMac(device.getDeviceMac());
        deviceInfoVO.setDeviceGatewayId(device.getDeviceGatewayId());
        deviceInfoVO.setDeviceTypeId(device.getDeviceTypeId());
        deviceInfoVO.setCommunicationModeItemId(device.getCommunicationModeItemId());
        deviceInfoVO.setDeviceNo(device.getDeviceNo());
        deviceInfoVO.setStatus(device.getStatus());
        deviceInfoVO.setRemark(device.getRemark());
        return deviceInfoVO;
    }

}
