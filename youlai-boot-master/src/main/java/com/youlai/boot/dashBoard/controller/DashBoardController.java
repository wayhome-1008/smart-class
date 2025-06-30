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
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import com.youlai.boot.device.model.influx.InfluxPlug;
import com.youlai.boot.device.model.influx.InfluxSensor;
import com.youlai.boot.device.model.vo.DeviceInfo;
import com.youlai.boot.device.model.vo.DeviceInfoVO;
import com.youlai.boot.device.model.vo.DeviceVO;
import com.youlai.boot.device.model.vo.InfluxMqttPlugVO;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.youlai.boot.common.util.DateUtils.findEarliest;
import static com.youlai.boot.common.util.DateUtils.findLatest;

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
        dashCount.setDemo1Count(logService.countWarning());
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
//                                                                                                        @RequestParam(defaultValue = "raw") String statsType
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
        return Result.failed();
    }

    @Operation(summary = "查询MQTT计量插座数据图数据")
    @GetMapping("/plugMqtt/data{id}")
    public Result<List<InfluxMqttPlugVO>> getMqttPlugData(
            @PathVariable Long id,

            @Parameter(description = "时间范围值", example = "1")
            @RequestParam Long timeAmount,

            @Parameter(description = "时间单位（y-年/mo-月/d-日/h-小时/m-分钟）", example = "d")
            @RequestParam(defaultValue = "h") String timeUnit
    ) {
        try {
            Device device = deviceService.getById(id);
            if (device == null) {
                return Result.failed();
            }
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .last(timeAmount, timeUnit)
                    .measurement("device")
                    .deviceCode(device.getDeviceCode())
                    .sort();
            buildFlexibleQuery(builder, timeAmount, timeUnit);
            log.info("influxdb查询MQTT计计量插座语句{}", builder.pivot().build());
            List<InfluxMqttPlug> tables = influxDBClient.getQueryApi().query(builder.pivot().build(), influxDBProperties.getOrg(), InfluxMqttPlug.class);
            //根据influxdb传来的数据把度数算上
            if (timeUnit.equals("y")) {
                //将12个月的数据转换
                return Result.success(makeInfluxMqttPlugVOList(tables));
            }
            if (timeUnit.equals("mo")) {
                //将30天数据转换
                return Result.success(makeInfluxMqttPlugVOList(tables));
            }
            if (timeUnit.equals("d")) {
                //将24小时数据转换
                return Result.success(makeInfluxMqttPlugVOList(tables));
            }
            if (timeUnit.equals("h")) {
                return Result.success(makeInfluxMqttPlugVOList(tables));
            }
            if (timeUnit.equals("m")) {
                return Result.success(makeInfluxMqttPlugVOList(tables));
            }
        } catch (InfluxException e) {
            System.err.println("error：" + e.getMessage());
        }
        return Result.failed();
    }

    private List<InfluxMqttPlugVO> makeInfluxMqttPlugVOList(List<InfluxMqttPlug> tables) {
        List<InfluxMqttPlugVO> influxMqttPlugVOList = new ArrayList<>();
        for (InfluxMqttPlug table : tables) {
            InfluxMqttPlugVO influxMqttPlugVO = new InfluxMqttPlugVO();
            influxMqttPlugVO.setTime(table.getTime().toString());
            influxMqttPlugVO.setValue(table.getTotal());
            influxMqttPlugVOList.add(influxMqttPlugVO);
        }
        return influxMqttPlugVOList;
    }

    @Operation(summary = "查询MQTT计量插座数据")
    @GetMapping("/plugMqtt/data")
    public Result<List<InfluxMqttPlug>> getMqttPlugData(@Parameter(description = "设备编码", required = true)
                                                        @RequestParam String deviceCode,

                                                        @Parameter(description = "时间范围值", example = "1")
                                                        @RequestParam Long timeAmount,

                                                        @Parameter(description = "时间单位（y-年/M-月/d-日/h-小时/m-分钟）", example = "d")
                                                        @RequestParam(defaultValue = "h") String timeUnit

//                                                    @Parameter(description = "统计类型（raw-原始数据/max-最大值/min-最小值/avg-平均值）", example = "raw")
//                                                    @RequestParam(defaultValue = "raw") String statsType
    ) {
        try {
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .last(timeAmount, timeUnit)
                    .measurement("device")
                    .deviceCode(deviceCode);
            buildFlexibleQuery(builder, timeAmount, timeUnit)
                    .sort()
                    .addFilter("r._field == \"Total\" or r._field == \"Yesterday\" or r._field==\"Today\" or r._field==\"Power\" or r._field==\"ApparentPower\" or r._field==\"ReactivePower\" or r._field==\"Factor\" or r._field==\"Voltage\" or r._field==\"Current\"");
            log.info("influxdb查询MQTT计计量插座语句{}", builder.pivot().build());
            List<InfluxMqttPlug> tables = influxDBClient.getQueryApi().query(builder.pivot().build(), influxDBProperties.getOrg(), InfluxMqttPlug.class);
            //根据influxdb传来的数据把度数算上
            if (timeUnit.equals("y")) {


            }
            if (timeUnit.equals("h")) {

                //用最新的total-最久的 total
                Double earliestValue = findEarliest(tables)
                        .map(InfluxMqttPlug::getTotal)
                        .orElse(null);

                Double latestValue = findLatest(tables)
                        .map(InfluxMqttPlug::getTotal)
                        .orElse(null);

                if (earliestValue != null && latestValue != null) {
                    //给所有tables对象的kilowattHour赋值 stream
//                    tables.forEach(t -> t.setKilowattHour(t.getTotal() - earliestValue));
                    double result = latestValue - earliestValue;
                    double rounded = Math.round(result * 1000) / 1000.0;
                    tables.get(0).setKilowattHour(rounded);
                }

            }
            if (timeUnit.equals("M")) {
                for (InfluxMqttPlug table : tables) {
                    table.setKilowattHour(table.getTotal());
                }
            }
            return Result.success(tables);
        } catch (InfluxException e) {
            System.err.println("error：" + e.getMessage());
        }
        return Result.failed();
    }

    private InfluxQueryBuilder buildFlexibleQuery(InfluxQueryBuilder builder, long timeAmount, String timeUnit) {
        // 根据时间单位动态设置窗口
        switch (timeUnit) {
            case "y": // 年 -> 按月分组
                builder.aggregateWindow("1mo", "sum")// InfluxDB中"mo"表示月
                        .addFilter("r._field == \"Total\" ");
                break;
            case "mo": // 月 -> 按天分组
                builder.aggregateWindow("1d", "sum").addFilter("r._field == \"Total\" ");
                break;
            case "d": // 日 -> 按小时分组
                builder.aggregateWindow("1h", "sum").addFilter("r._field == \"Total\" ");
                break;
            case "h": // 小时 -> 按分钟分组
                builder.aggregateWindow("1m", "sum").addFilter("r._field == \"Total\" ");
                break;
            case "m": // 分钟 -> 按秒分组（可选）
                builder.aggregateWindow("1s", "sum").addFilter("r._field == \"Total\" ");
                break;
        }
        return builder;
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
