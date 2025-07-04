package com.youlai.boot.dashBoard.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import com.youlai.boot.device.model.influx.InfluxHumanRadarSensor;
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import com.youlai.boot.device.model.influx.InfluxSensor;
import com.youlai.boot.device.model.influx.InfluxSwitch;
import com.youlai.boot.device.model.vo.*;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;

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

    @Operation(summary = "房间当天用电信息")
    @GetMapping("/room/electricity")
    public Result<RoomElectricity> getRoomElectricityData(
            @Parameter(description = "房间id")
            @RequestParam Long roomId) {
        try {
            // 使用新的InfluxQueryBuilder构建查询
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .range(1, "d")
                    .measurement("device")
                    .fields("Total", "Voltage", "Current")
                    .pivot()
                    .fill()
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .limit(1)
                    .timeShift("8h");
            builder.tag("roomId", String.valueOf(roomId));
            String fluxQuery = builder.build();
            log.info("InfluxDB查询语句: {}", fluxQuery);

            List<InfluxMqttPlug> tables = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);         //根据influxdb传来的数据把度数算上
            if (!tables.isEmpty()) {
                InfluxMqttPlug latestData = tables.get(0);
                RoomElectricity result = new RoomElectricity();
                result.setTotal(latestData.getTotal());
                result.setVoltage(latestData.getVoltage());
                result.setCurrent(latestData.getCurrent());
                return Result.success(result);
            }
            return Result.failed("未找到用电数据");
        } catch (InfluxException e) {
            log.error("查询用电数据失败: {}", e.getMessage());
            return Result.failed("查询用电数据失败");
        }
    }

    @Operation(summary = "查询计量插座数据图数据")
    @GetMapping("/plugMqtt/data")
    public Result<List<InfluxMqttPlugVO>> getMqttPlugData(
            @Parameter(description = "设备编码")
            @RequestParam(required = false) String deviceCode,

            @Parameter(description = "时间范围值", example = "1")
            @RequestParam Long timeAmount,

            @Parameter(description = "时间单位（y-年/mo-月/w-周/d-日/h-小时/m-分钟）", example = "d")
            @RequestParam(defaultValue = "h") String timeUnit,

            @Parameter(description = "房间id")
            @RequestParam(required = false) String roomId,

            @Parameter(description = "设备类型id")
            @RequestParam(required = false) String deviceTypeId

    ) {
        try {
            // 使用新的InfluxQueryBuilder构建查询
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .range(timeAmount, timeUnit)
                    .measurement("device")
                    .fields("Total", "Voltage", "Current")
                    .pivot()
                    .fill()
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .timeShift("8h");

            // 添加设备编码和房间ID过滤条件
            if (StringUtils.isNotBlank(deviceCode)) {
                builder.tag("deviceCode", deviceCode);
            }
            if (StringUtils.isNotBlank(roomId)) {
                builder.tag("roomId", roomId);
            }
            if (StringUtils.isNotBlank(deviceTypeId)) {
                builder.tag("deviceType", deviceTypeId);
            }

            // 根据时间单位设置窗口聚合
            switch (timeUnit) {
                case "y":
                    builder.window("1mo", "last");
                    break;
                case "mo":
                    builder.window("1d", "last");
                    break;
                case "w":  // 新增周的处理
                    builder.window("1d", "last");
                    break;
                case "d":
                    builder.window("1h", "last");
                    break;
                case "h":
                    builder.window("1m", "last");
                    break;
                case "m":
                    builder.window("1s", "last");
                    break;
            }
            String fluxQuery = builder.build();
            log.info("InfluxDB查询语句: {}", fluxQuery);
            List<InfluxMqttPlug> tables = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);            //根据influxdb传来的数据把度数算上
            // 转换结果并返回
            return Result.success(makeInfluxMqttPlugVOList(tables, timeUnit));
        } catch (InfluxException e) {
            System.err.println("error：" + e.getMessage());
        }
        return Result.failed();
    }

//    @Operation(summary = "查询房间用电数据")
//    @GetMapping("/roomPower/data")
//    public Result<List<InfluxMqttPlugVO>> getRoomPowerData(
//
//            @Parameter(description = "房间id")
//            @RequestParam(required = true) String roomId
//    ) {
//        try {
//            // 使用新的InfluxQueryBuilder构建查询/deviceType/options
//            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
//                    .bucket(influxDBProperties.getBucket())
//                    .range(timeAmount, timeUnit)
//                    .measurement("device")
//                    .fields("Total", "Voltage", "Current")
//                    .pivot()
//                    .fill()
//                    .sort("_time", InfluxQueryBuilder.SORT_DESC)
//                    .timeShift("8h");
//
//            // 添加设备编码和房间ID过滤条件
//            if (StringUtils.isNotBlank(deviceCode)) {
//                builder.tag("deviceCode", deviceCode);
//            }
//            if (StringUtils.isNotBlank(roomId)) {
//                builder.tag("roomId", roomId);
//            }
//
//            // 根据时间单位设置窗口聚合
//            switch (timeUnit) {
//                case "y":
//                    builder.window("1mo", "last");
//                    break;
//                case "mo":
//                    builder.window("1d", "last");
//                    break;
//                case "d":
//                    builder.window("1h", "last");
//                    break;
//                case "h":
//                    builder.window("1m", "last");
//                    break;
//                case "m":
//                    builder.window("1s", "last");
//                    break;
//            }
//            String fluxQuery = builder.build();
//            log.info("InfluxDB查询语句: {}", fluxQuery);
//            List<InfluxMqttPlug> tables = influxDBClient.getQueryApi()
//                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);            //根据influxdb传来的数据把度数算上
//            // 转换结果并返回
//            return Result.success(makeInfluxMqttPlugVOList(tables, timeUnit));
//        } catch (InfluxException e) {
//            System.err.println("error：" + e.getMessage());
//        }
//        return Result.failed();
//    }

    @Operation(summary = "查询传感器数据")
    @GetMapping("/sensor/data")
    public Result<List<InfluxSensorVO>> getSensorData(
            @Parameter(description = "设备编码", required = true)
            @RequestParam String deviceCode,

            @Parameter(description = "时间范围值", example = "1")
            @RequestParam Long timeAmount,

            @Parameter(description = "时间单位（y-年/M-月/d-日/h-小时）", example = "d")
            @RequestParam(defaultValue = "h") String timeUnit,

            @Parameter(description = "房间id")
            @RequestParam(required = false) String roomId
    ) {
        try {
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .range(timeAmount, timeUnit)
                    .measurement("device")
                    .fields("temperature", "humidity", "illuminance", "battery")
                    .pivot()
                    .fill()
                    .sort("_time", InfluxQueryBuilder.SORT_DESC)
                    .timeShift("8h");
            // 添加设备编码和房间ID过滤条件
            if (StringUtils.isNotBlank(deviceCode)) {
                builder.tag("deviceCode", deviceCode);
            }
            if (StringUtils.isNotBlank(roomId)) {
                builder.tag("roomId", roomId);
            }
            // 根据时间单位设置窗口聚合
            switch (timeUnit) {
                case "y":
                    builder.window("1mo", "mean");
                    break;
                case "mo":
                    builder.window("1d", "mean");
                    break;
                case "d":
                    builder.window("1h", "mean");
                    break;
                case "h":
                    builder.window("1m", "mean");
                    break;
                case "m":
                    builder.window("1s", "mean");
                    break;
            }
            String fluxQuery = builder.build();
            log.info("influxdb查询传感器语句{}", fluxQuery);
            List<InfluxSensor> tables = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxSensor.class);
            return Result.success(makeInfluxSensorVOList(tables, timeUnit));
        } catch (InfluxException e) {
            System.err.println("error：" + e.getMessage());
        }
        return Result.failed();
    }

    private List<InfluxSensorVO> makeInfluxSensorVOList(List<InfluxSensor> tables, String timeUnit) {
        List<InfluxSensorVO> result = new ArrayList<>(3);

        // 初始化三个VO对象，分别对应温度、湿度、光照
        InfluxSensorVO tempVO = new InfluxSensorVO();
        tempVO.setName("temperature");
        List<String> tempTimes = new ArrayList<>();
        List<Double> tempValues = new ArrayList<>();

        InfluxSensorVO humidityVO = new InfluxSensorVO();
        humidityVO.setName("humidity");
        List<String> humidityTimes = new ArrayList<>();
        List<Double> humidityValues = new ArrayList<>();

        InfluxSensorVO illuminanceVO = new InfluxSensorVO();
        illuminanceVO.setName("illuminance");
        List<String> illuminanceTimes = new ArrayList<>();
        List<Double> illuminanceValues = new ArrayList<>();

        // 遍历查询结果，分别填充数据
        for (InfluxSensor table : tables) {
            String formattedTime = formatTime(table.getTime(), timeUnit);

            // 温度数据
            tempTimes.add(formattedTime);
            tempValues.add(table.getTemperature());

            // 湿度数据
            humidityTimes.add(formattedTime);
            humidityValues.add(table.getHumidity());

            // 光照数据
            illuminanceTimes.add(formattedTime);
            illuminanceValues.add(table.getIlluminance());
        }

        // 设置VO对象的值
        tempVO.setTime(tempTimes);
        tempVO.setValue(tempValues);

        humidityVO.setTime(humidityTimes);
        humidityVO.setValue(humidityValues);

        illuminanceVO.setTime(illuminanceTimes);
        illuminanceVO.setValue(illuminanceValues);

        // 添加到结果列表
        result.add(tempVO);
        result.add(humidityVO);
        result.add(illuminanceVO);

        return result;
    }

    @Operation(summary = "查询人体雷达数据图数据")
    @GetMapping("/motion/data")
    public Result<InfluxMotionVO> getMqttPlugData(
            @Parameter(description = "设备编码")
            @RequestParam(required = false) String deviceCode,

            @Parameter(description = "房间id")
            @RequestParam(required = false) String roomId

    ) {
        try {
            // 使用新的InfluxQueryBuilder构建查询
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .range(1, "d")
                    .measurement("device")
                    .fields("motion")
                    .pivot()
                    .fill()
                    .sort("_time", InfluxQueryBuilder.SORT_DESC)
                    .timeShift("8h");
            // 添加设备编码和房间ID过滤条件
            if (StringUtils.isNotBlank(deviceCode)) {
                builder.tag("deviceCode", deviceCode);
            }
            if (StringUtils.isNotBlank(roomId)) {
                builder.tag("roomId", roomId);
            }
            // 根据时间单位设置窗口聚合
            builder.window("5m", "last");
            String fluxQuery = builder.build();
            log.info("InfluxDB查询语句: {}", fluxQuery);
            List<InfluxHumanRadarSensor> tables = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxHumanRadarSensor.class);            //根据influxdb传来的数据把度数算上
            // 转换结果并返回
            return Result.success(makeInfluxMotionVOList(tables));
        } catch (
                InfluxException e) {
            System.err.println("error：" + e.getMessage());
        }
        return Result.failed();
    }

    @Operation(summary = "查询开关数据")
    @GetMapping("/switch/data")
    public Result<List<InfluxSwitchVO>> getSwitchData(
            @Parameter(description = "设备编码", required = true)
            @RequestParam String deviceCode,

            @Parameter(description = "房间id")
            @RequestParam(required = false) String roomId
    ) throws JsonProcessingException {
        try {
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .range(1, "d")
                    .measurement("device")
                    .fields("switch")
                    .pivot()
                    .fill()
                    .sort("_time", InfluxQueryBuilder.SORT_DESC)
                    .timeShift("8h");
            // 添加设备编码和房间ID过滤条件
            if (StringUtils.isNotBlank(deviceCode)) {
                builder.tag("deviceCode", deviceCode);
            }
            if (StringUtils.isNotBlank(roomId)) {
                builder.tag("roomId", roomId);
            }
            String fluxQuery = builder.build();
            log.info("influxdb查询传感器语句{}", fluxQuery);
            List<InfluxSwitch> tables = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxSwitch.class);
            List<InfluxSwitchVO> result = new ArrayList<>();
            for (InfluxSwitch table : tables) {
                JsonNode jsonNode = stringToJsonNode(table.getSwitchState());
                Iterator<String> fieldNames = jsonNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    if (fieldName.startsWith("switch")) {
                        String status = jsonNode.get(fieldName).asText();
                        InfluxSwitchVO switchVO = new InfluxSwitchVO();
                        switchVO.setWay(fieldName);
                        switchVO.setSwitchStatus(status);
                        // 格式化时间为 年月日时分秒
                        switchVO.setTime(table.getTime());
                        result.add(switchVO);
                    }
                }
            }
            //对状态解析
            return Result.success(result);
        } catch (InfluxException e) {
            System.err.println("error：" + e.getMessage());
        }
        return Result.failed();
    }

    private InfluxMotionVO makeInfluxMotionVOList(List<InfluxHumanRadarSensor> tables) {
        InfluxMotionVO result = new InfluxMotionVO();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (InfluxHumanRadarSensor table : tables) {
            //把Instant转时分秒字符串
            String time = formatTime(table.getTime(), "HH:mm:ss");
            times.add(time);
            values.add(table.getMotion() != null && table.getMotion() == 1d ? 1d : 0d);
        }
        result.setTime(times);
        result.setValue(values);
        return result;
    }


    private List<InfluxMqttPlugVO> makeInfluxMqttPlugVOList(List<InfluxMqttPlug> tables, String timeUnit) {
        List<InfluxMqttPlugVO> result = new ArrayList<>();
        InfluxMqttPlugVO vo = new InfluxMqttPlugVO();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (InfluxMqttPlug table : tables) {
            // 格式化时间（根据你的需求选择格式）
            String formattedTime = formatTime(table.getTime(), timeUnit);
            times.add(formattedTime);
            values.add(table.getTotal());
        }
        vo.setTime(times);
        vo.setValue(values);
        result.add(vo);
        return result;
    }

    private String formatTime(Instant time, String timeUnit) {
        ZoneId utcZone = ZoneId.of("UTC");
        return switch (timeUnit) {
            case "y" -> time.atZone(utcZone).getYear() + "/" +
                    String.format("%02d", time.atZone(utcZone).getMonthValue());
            case "mo" -> String.format("%02d", time.atZone(utcZone).getMonthValue()) + "/" +
                    String.format("%02d", time.atZone(utcZone).getDayOfMonth());
            case "w" ->  // 修改为显示星期几
                    time.atZone(utcZone).getDayOfWeek().toString(); // 直接返回DayOfWeek枚举名称
            case "d" -> String.format("%02d", time.atZone(utcZone).getHour()) + ":00";
            case "h" -> String.format("%02d", time.atZone(utcZone).getMinute()) + ":00";
            case "HH:mm:ss" -> String.format("%02d:%02d:%02d",
                    time.atZone(utcZone).getHour(),
                    time.atZone(utcZone).getMinute(),
                    time.atZone(utcZone).getSecond());
            default -> time.atZone(utcZone).format(DateTimeFormatter.ISO_LOCAL_TIME);
        };
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
