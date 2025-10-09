package com.youlai.boot.dashBoard.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import com.youlai.boot.alertEvent.service.AlertEventService;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.InfluxQueryBuilder;
import com.youlai.boot.common.util.MathUtils;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.dashBoard.model.vo.*;
import com.youlai.boot.dashBoard.service.ElectricityCalculationService;
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
import com.youlai.boot.system.model.entity.Config;
import com.youlai.boot.system.service.ConfigService;
import com.youlai.boot.system.service.LogService;
import com.youlai.boot.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.youlai.boot.common.util.JsonUtils.stringToJsonNode;
import static com.youlai.boot.common.util.MathUtils.formatDouble;

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
    private final ConfigService configService;
    private final CategoryService categoryService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ElectricityCalculationService electricityCalculationService;
    private final AlertEventService alertEventService;

    @Operation(summary = "查询配置分类用电量")
    @GetMapping("/category/electricity")
    public Result<CategoryElectricityVO> getCategoryElectricityData() {
        // 1. 初始化结果对象
        CategoryElectricityVO result = new CategoryElectricityVO();
        List<CategoryElectricityVO.CategoryData> categoryDataList = new ArrayList<>();

        // 2. 获取所有分类
        List<Category> categories = categoryService.list();
        List<Category> categoriesAll = new ArrayList<>();
        if (ObjectUtils.isEmpty(categories)) return Result.success();

        List<String> categoryNames = categories.stream().map(Category::getCategoryName).toList();
        List<Config> configList = configService.listByKeys(categoryNames);
        for (Config config : configList) {
            for (String categoryName : categoryNames) {
                if (categoryName.equals(config.getConfigKey())) {
                    categoriesAll.add(categories.stream()
                            .filter(category1 -> category1.getCategoryName().equals(categoryName))
                            .findFirst()
                            .orElse(null));
                }
            }
        }

        // 4. 处理每个分类
        for (Category category : categoriesAll) {
            // 根据categoryId和isMaster查询设备
            List<Device> masterDevices = deviceService.listByCategoryId(category);
            if (!masterDevices.isEmpty()) {
                CategoryElectricityVO.CategoryData data = new CategoryElectricityVO.CategoryData();
                data.setCategoryName(category.getCategoryName());
                // 初始化总值列表，用于累加所有设备的数据
                List<Double> totalValues = null;
                List<String> times = null;
                // 遍历所有主设备，获取并累加数据
                for (Device masterDevice : masterDevices) {
                    // 使用新方法获取最近7天的每日用电量数据
                    CategoryElectricityData deviceData =
                            electricityCalculationService.getWeeklyElectricityDataForCategory(masterDevice, "");
                    if (times == null) {
                        times = deviceData.getTime();
                    }
                    List<Double> deviceValues = deviceData.getValue();
                    if (totalValues == null) {
                        // 第一个设备的数据作为初始值
                        totalValues = new ArrayList<>(deviceValues);
                    } else {
                        // 确保两个列表长度相同后再累加
                        int minSize = Math.min(totalValues.size(), deviceValues.size());
                        for (int i = 0; i < minSize; i++) {
                            Double v1 = totalValues.get(i);
                            Double v2 = deviceValues.get(i);
                            // 处理null值
                            if (v1 == null) v1 = 0.0;
                            if (v2 == null) v2 = 0.0;
                            totalValues.set(i, MathUtils.formatDouble(v1 + v2));
                        }
                    }
                }
                // 设置时间轴（只需要设置一次）
                if (result.getTimes() == null || result.getTimes().isEmpty()) {
                    result.setTimes(times);
                }
                data.setValues(totalValues);
                categoryDataList.add(data);
            }
        }
        result.setData(categoryDataList);
        return Result.success(result);
    }

    @Operation(summary = "获取首页count数量")
    @GetMapping("/count")
    public Result<DashCount> getDashCount() {
        DashCount dashCount = new DashCount();
        dashCount.setDeviceCount(deviceService.count());
        dashCount.setUserCount(userService.count());
        dashCount.setLogCount(logService.count());
        dashCount.setRoomCount(roomService.count());
        dashCount.setDemo1Count(alertEventService.warningCount());
        dashCount.setDemo2Count(alertEventService.warningUnhandlerCount());
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
        // 优先从Redis缓存获取设备信息
        Device deviceCache = (Device) redisTemplate.opsForHash().get(RedisConstants.Device.DEVICE, code);
        if (ObjectUtils.isNotEmpty(deviceCache)) {
            // 根据roomId查询房间信息
            Room room = roomService.getById(deviceCache.getDeviceRoom());
            DeviceInfoVO deviceInfoVO = basicPropertyConvert(deviceCache, room.getClassroomCode());

            // 使用工厂对设备具体信息转换
            String deviceType = DeviceTypeEnum.getNameById(deviceCache.getDeviceTypeId());
            String communicationMode = CommunicationModeEnum.getNameById(deviceCache.getCommunicationModeItemId());
            DeviceInfoParser parser = DeviceInfoParserFactory.getParser(deviceType, communicationMode);
            List<DeviceInfo> deviceInfos = parser.parse(deviceCache.getDeviceInfo());
            deviceInfoVO.setDeviceInfo(deviceInfos);

            return Result.success(deviceInfoVO);
        } else {
            // 缓存中没有则从数据库查询
            Device device = deviceService.getByCode(code);
            if (ObjectUtils.isNotEmpty(device)) {
                // 根据roomId查询房间信息
                Room room = roomService.getById(device.getDeviceRoom());
                DeviceInfoVO deviceInfoVO = basicPropertyConvert(device, room.getClassroomCode());

                // 使用工厂对设备具体信息转换
                String deviceType = DeviceTypeEnum.getNameById(device.getDeviceTypeId());
                String communicationMode = CommunicationModeEnum.getNameById(device.getCommunicationModeItemId());
                DeviceInfoParser parser = DeviceInfoParserFactory.getParser(deviceType, communicationMode);
                List<DeviceInfo> deviceInfos = parser.parse(device.getDeviceInfo());
                deviceInfoVO.setDeviceInfo(deviceInfos);

                return Result.success(deviceInfoVO);
            }
            return Result.failed("设备不存在");
        }
    }


    @Operation(summary = "根据设备code获取数据")
    @GetMapping("/id/{id}/info")
    public Result<DeviceInfoVO> getDeviceInfoById(@PathVariable Long id) {
        Device device = deviceService.getById(id);
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

    @Operation(summary = "房间当天用电信息(仅显示总用电、功率、电压)")
    @GetMapping("/room/electricity")
    public Result<RoomElectricity> getRoomElectricityData(
            @Parameter(description = "房间id")
            @RequestParam Long roomId) {
        try {
            // 查询当天该房间所有设备的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .today()
                    .measurement("device")
                    .fields("Total", "Voltage", "Current")
                    .pivot()
                    .fill()
                    .sort("_time", InfluxQueryBuilder.SORT_ASC);
            builder.tag("roomId", String.valueOf(roomId));

            String fluxQuery = builder.build();
            log.info("房间当天用电数据InfluxDB查询语句: {}", fluxQuery);

            List<InfluxMqttPlug> allDeviceData = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (allDeviceData.isEmpty()) {
                return Result.success();
            }

            // 按设备分组数据
            Map<String, List<InfluxMqttPlug>> deviceDataMap = allDeviceData.stream()
                    .filter(data -> data.getDeviceCode() != null)
                    .collect(Collectors.groupingBy(InfluxMqttPlug::getDeviceCode));

            // 计算每个设备的用电量差值
            double roomTotalElectricity = 0.0;
            Double avgVoltage = null;
            Double avgCurrent = null;
            int voltageCurrentCount = 0;

            for (Map.Entry<String, List<InfluxMqttPlug>> entry : deviceDataMap.entrySet()) {
                List<InfluxMqttPlug> deviceDataList = entry.getValue();

                // 按时间排序
                deviceDataList.sort(Comparator.comparing(InfluxMqttPlug::getTime));

                if (deviceDataList.size() >= 2) {
                    // 获取最早和最晚的数据点
                    InfluxMqttPlug earliestData = deviceDataList.get(0);
                    InfluxMqttPlug latestData = deviceDataList.get(deviceDataList.size() - 1);

                    // 计算单个设备的用电量差值
                    if (earliestData.getTotal() != null && latestData.getTotal() != null) {
                        double deviceConsumption = latestData.getTotal() - earliestData.getTotal();
                        roomTotalElectricity += Math.max(0, formatDouble(deviceConsumption));
                    }

                    // 累加电压和电流用于计算平均值
                    if (latestData.getVoltage() != null) {
                        if (avgVoltage == null) {
                            avgVoltage = 0.0;
                        }
                        avgVoltage += latestData.getVoltage();
                    }

                    if (latestData.getCurrent() != null) {
                        if (avgCurrent == null) {
                            avgCurrent = 0.0;
                        }
                        avgCurrent += latestData.getCurrent();
                    }

                    if (latestData.getVoltage() != null || latestData.getCurrent() != null) {
                        voltageCurrentCount++;
                    }
                } else if (deviceDataList.size() == 1) {
                    // 如果只有一个数据点，使用该点的数据（向后兼容）
                    InfluxMqttPlug data = deviceDataList.get(0);
                    if (data.getTotal() != null) {
                        roomTotalElectricity += formatDouble(data.getTotal());
                    }

                    // 累加电压和电流用于计算平均值
                    if (data.getVoltage() != null) {
                        if (avgVoltage == null) {
                            avgVoltage = 0.0;
                        }
                        avgVoltage += data.getVoltage();
                    }

                    if (data.getCurrent() != null) {
                        if (avgCurrent == null) {
                            avgCurrent = 0.0;
                        }
                        avgCurrent += data.getCurrent();
                    }

                    if (data.getVoltage() != null || data.getCurrent() != null) {
                        voltageCurrentCount++;
                    }
                }
            }

            RoomElectricity result = new RoomElectricity();
            result.setTotal(formatDouble(roomTotalElectricity));

            // 计算平均电压和电流
            if (voltageCurrentCount > 0) {
                if (avgVoltage != null) {
                    result.setVoltage(formatDouble(avgVoltage / voltageCurrentCount));
                }
                if (avgCurrent != null) {
                    result.setCurrent(formatDouble(avgCurrent / voltageCurrentCount));
                }
            }

            return Result.success(result);

        } catch (InfluxException e) {
            log.error("查询用电数据失败: {}", e.getMessage());
            return Result.failed("查询用电数据失败");
        } catch (Exception e) {
            log.error("处理用电数据时发生错误: ", e);
            return Result.failed("系统错误");
        }
    }

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
        tables.sort(Comparator.comparing(InfluxSensor::getTime));
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
            tempValues.add(formatDouble(table.getTemperature()));

            // 湿度数据
            humidityTimes.add(formattedTime);
            humidityValues.add(formatDouble(table.getHumidity()));

            // 光照数据
            illuminanceTimes.add(formattedTime);
            illuminanceValues.add(formatDouble(table.getIlluminance()));
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
                    .today()  // 使用当天范围
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
            log.info("查询人体雷达数据图数据InfluxDB查询语句: {}", fluxQuery);
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
    public PageResult<InfluxSwitchVO> getSwitchData(
            @Parameter(description = "设备编码", required = true)
            @RequestParam String deviceCode,

            @Parameter(description = "房间id")
            @RequestParam(required = false) String roomId,

            @Parameter(description = "页码，默认1")
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页数量，默认10")
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
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
            // 计算偏移量并设置分页
            int offset = (pageNum - 1) * pageSize;
            builder.limit(pageSize).offset(offset);
            String fluxQuery = builder.build();
            log.info("influxdb查询开关语句{}", fluxQuery);
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
                        switchVO.setTime(formatTime(table.getTime(), "yyyy-MM-dd HH:mm:ss"));
                        result.add(switchVO);
                    }
                }
            }
            // 计算总记录数(需要单独查询)
            long total = getTotalCount(deviceCode, roomId);
            Page<InfluxSwitchVO> page = new Page<>();
            page.setTotal(total);
            page.setRecords(result);
            page.setCurrent(pageNum);
            page.setSize(pageSize);

            return PageResult.success(page);
        } catch (InfluxException e) {
            System.err.println("error：" + e.getMessage());
        }
//        return PageResult.failed();
        return null;
    }

    @Operation(summary = "查询各房间总用电量排名(升序)")
    @GetMapping("/electricity/ranking")
    public Result<List<RoomsElectricityVO>> getRoomElectricityRankingAsc(
            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime,
            @Parameter(description = "时间范围: today-今天", example = "today")
            @RequestParam(defaultValue = "today") String range) {
        try {
            // 1. 获取所有房间列表
            List<Room> rooms = roomService.list();
            String roomIds = rooms.stream().map(Room::getId).map(String::valueOf).collect(Collectors.joining(","));
            // 2. 构建结果列表
            // 3. 只处理today情况
            if (!"today".equals(range)) {
                return Result.failed("只支持today时间范围参数");
            }
            PageResult<RoomsElectricityVO> roomsElectricityVOPageResult = electricityCalculationService.getRoomsElectricityVOPageResult(1, 10, roomIds, startTime, endTime, range, "", false);
            List<RoomsElectricityVO> list = roomsElectricityVOPageResult.getData().getList();
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询房间用电量排名失败: {}", e.getMessage(), e);
            return Result.failed("查询房间用电量排名失败");
        }
    }

    // 辅助方法：获取总记录数
    private long getTotalCount(String deviceCode, String roomId) {
        InfluxQueryBuilder countBuilder = InfluxQueryBuilder.newBuilder()
                .bucket(influxDBProperties.getBucket())
                .range(1, "d")
                .measurement("device")
                .fields("switch")
                .count();

        if (StringUtils.isNotBlank(deviceCode)) {
            countBuilder.tag("deviceCode", deviceCode);
        }
        if (StringUtils.isNotBlank(roomId)) {
            countBuilder.tag("roomId", roomId);
        }

        String countQuery = countBuilder.build();
        log.debug("InfluxDB查询总记录数语句: {}", countQuery);

        try {
            List<CountResult> results = influxDBClient.getQueryApi()
                    .query(countQuery, influxDBProperties.getOrg(), CountResult.class);
            return results.isEmpty() ? 0 : results.get(0).getValue();
        } catch (Exception e) {
            log.error("查询总记录数失败", e);
            return 0;
        }
    }

    private InfluxMotionVO makeInfluxMotionVOList(List<InfluxHumanRadarSensor> tables) {
        tables.sort(Comparator.comparing(InfluxHumanRadarSensor::getTime));
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


    @SuppressWarnings("Duplicates")
    public static DeviceInfoVO basicPropertyConvert(Device device, String roomCode) {
        DeviceInfoVO deviceInfoVO = new DeviceInfoVO();
        deviceInfoVO.setId(device.getId());
        deviceInfoVO.setDeviceName(device.getDeviceName());
        deviceInfoVO.setDeviceCode(device.getDeviceCode());
        deviceInfoVO.setDeviceRoom(device.getDeviceRoom());
        deviceInfoVO.setRoomName(roomCode);
        deviceInfoVO.setDeviceMac(device.getDeviceMac());
        deviceInfoVO.setDeviceGatewayId(device.getDeviceGatewayId());
        deviceInfoVO.setCategoryId(device.getCategoryId());
        deviceInfoVO.setDeviceTypeId(device.getDeviceTypeId());
        deviceInfoVO.setCommunicationModeItemId(device.getCommunicationModeItemId());
        deviceInfoVO.setDeviceNo(device.getDeviceNo());
        deviceInfoVO.setStatus(device.getStatus());
        deviceInfoVO.setRemark(device.getRemark());
        return deviceInfoVO;
    }

    @SuppressWarnings("Duplicates")
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

    @Operation(summary = "查询计量插座数据图数据")
    @GetMapping("/plugMqtt/data")
    public Result<List<CategoryElectricityData>> getMqttPlugData(
            @Parameter(description = "设备编码")
            @RequestParam(required = false) String deviceCode,

            @Parameter(description = "时间范围值", example = "1")
            @RequestParam Long timeAmount,

            @Parameter(description = "时间单位（y-年/mo-月/w-周/d-日/h-小时/m-分钟）", example = "d")
            @RequestParam(defaultValue = "h") String timeUnit,

            @Parameter(description = "房间id")
            @RequestParam(required = false) String roomId,

            @Parameter(description = "设备类型ids")
            @RequestParam(required = false) String deviceTypeIds

    ) {
        try {
            // 2. 根据传入的参数查询设备列表
            List<Device> deviceList;
            LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Device::getDeviceTypeId, 4, 8);
            // 添加查询条件
            if (StringUtils.isNotBlank(deviceCode)) {
                queryWrapper.eq(Device::getDeviceCode, deviceCode);
            }
            if (StringUtils.isNotBlank(roomId)) {
                queryWrapper.eq(Device::getDeviceRoom, roomId);
            }
            deviceList = deviceService.list(queryWrapper);
            CategoryElectricityData electricityDataForCategory = electricityCalculationService.getElectricityDataForCategory(deviceList, timeUnit, roomId);
            return Result.success(List.of(electricityDataForCategory));
        } catch (InfluxException e) {
            log.error("InfluxDB查询异常", e);
            return Result.failed("数据查询失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("接口处理异常", e);
            return Result.failed("系统异常");
        }
    }

    /**
     * 时间格式化（根据时间单位显示不同格式）
     */
    private String formatTime(Instant time, String timeUnit) {
        // 使用北京时间
        ZoneId beijingZone = ZoneId.of("Asia/Shanghai");
        return switch (timeUnit) {
            case "y" -> time.atZone(beijingZone).getYear() + "/" +
                    String.format("%02d", time.atZone(beijingZone).getMonthValue());
            case "mo" -> String.format("%02d", time.atZone(beijingZone).getMonthValue()) + "/" +
                    String.format("%02d", time.atZone(beijingZone).getDayOfMonth());
            case "w" -> {
                Instant now = Instant.now();
                Instant currentMonday = now.atZone(beijingZone)
                        .with(java.time.DayOfWeek.MONDAY)
                        .toLocalDate()
                        .atStartOfDay(beijingZone)
                        .toInstant();
                String prefix = time.isBefore(currentMonday) ? "上" : "";
                int dayOfWeek = time.atZone(beijingZone).getDayOfWeek().getValue();
                yield switch (dayOfWeek) {
                    case 1 -> prefix + "周一";
                    case 2 -> prefix + "周二";
                    case 3 -> prefix + "周三";
                    case 4 -> prefix + "周四";
                    case 5 -> prefix + "周五";
                    case 6 -> prefix + "周六";
                    case 7 -> prefix + "周日";
                    default -> String.valueOf(dayOfWeek);
                };
            }
            case "d" -> String.format("%02d", time.atZone(beijingZone).getHour()) + ":00";
            case "h" -> String.format("%02d:%02d",
                    time.atZone(beijingZone).getHour(),
                    time.atZone(beijingZone).getMinute());
            case "m" -> String.format("%02d:%02d:%02d",
                    time.atZone(beijingZone).getHour(),
                    time.atZone(beijingZone).getMinute(),
                    time.atZone(beijingZone).getSecond());
            default -> time.atZone(beijingZone).format(DateTimeFormatter.ISO_LOCAL_TIME);
        };
    }

}
