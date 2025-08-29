package com.youlai.boot.dashBoard.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;
import com.youlai.boot.categoryDeviceRelationship.service.CategoryDeviceRelationshipService;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.InfluxQueryBuilder;
import com.youlai.boot.common.util.MathUtils;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.dashBoard.model.vo.CategoryElectricityVO;
import com.youlai.boot.dashBoard.model.vo.CountResult;
import com.youlai.boot.dashBoard.model.vo.DashCount;
import com.youlai.boot.dashBoard.model.vo.RoomElectricityRankingVO;
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
import java.time.LocalDate;
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
    private final CategoryDeviceRelationshipService categoryDeviceRelationshipService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ElectricityCalculationService electricityCalculationService;

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
            List<CategoryDeviceRelationship> relationships =
                    categoryDeviceRelationshipService.listByCategoryId(category.getId());
            if (!relationships.isEmpty()) {
                List<Long> deviceIds = relationships.stream()
                        .map(CategoryDeviceRelationship::getDeviceId)
                        .collect(Collectors.toList());

                List<Device> masterDevices = deviceService.listByIds(deviceIds).stream()
                        .filter(device -> device.getIsMaster() == 1)
                        .toList();

                if (!masterDevices.isEmpty()) {
                    CategoryElectricityVO.CategoryData data = new CategoryElectricityVO.CategoryData();
                    data.setCategoryName(category.getCategoryName());

                    // 初始化总值列表，用于累加所有设备的数据
                    List<Double> totalValues = null;

                    // 遍历所有主设备，获取并累加数据
                    for (Device masterDevice : masterDevices) {
                        List<InfluxMqttPlugVO> deviceDataList = querySingleDevice(masterDevice.getDeviceCode(), 1L, "w", "1d", null, null);

                        if (!deviceDataList.isEmpty()) {
                            InfluxMqttPlugVO deviceData = deviceDataList.get(0);

                            if (totalValues == null) {
                                // 第一个设备的数据作为初始值
                                totalValues = new ArrayList<>(deviceData.getValue());
                                result.setTimes(deviceData.getTime());
                            } else {
                                // 确保两个列表长度相同后再累加
                                List<Double> deviceValues = deviceData.getValue();
                                int minSize = Math.min(totalValues.size(), deviceValues.size());
                                for (int i = 0; i < minSize; i++) {
                                    Double v1 = MathUtils.formatDouble(totalValues.get(i));
                                    Double v2 = MathUtils.formatDouble(deviceValues.get(i));
                                    // 处理null值
                                    if (v1 == null) v1 = 0.0;
                                    if (v2 == null) v2 = 0.0;
                                    totalValues.set(i, v1 + v2);
                                }
                            }
                        }
                    }

                    data.setValues(totalValues);
                    categoryDataList.add(data);
                }
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
        dashCount.setDemo1Count(logService.countWarning());
        dashCount.setDemo2Count((Integer) redisTemplate.opsForValue().get(RedisConstants.MessageCount.MESSAGE_COUNT_KEY));
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
            // 查询当天最早的数据点
            InfluxQueryBuilder earliestBuilder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .today()
                    .measurement("device")
                    .fields("Total", "Voltage", "Current")
                    .pivot()
                    .fill()
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .limit(1);
            earliestBuilder.tag("roomId", String.valueOf(roomId));

            // 查询当天最晚的数据点
            InfluxQueryBuilder latestBuilder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .today()
                    .measurement("device")
                    .fields("Total", "Voltage", "Current")
                    .pivot()
                    .fill()
                    .sort("_time", InfluxQueryBuilder.SORT_DESC)
                    .limit(1);
            latestBuilder.tag("roomId", String.valueOf(roomId));

            String earliestFluxQuery = earliestBuilder.build();
            String latestFluxQuery = latestBuilder.build();

            log.info("房间当天用电最早数据InfluxDB查询语句: {}", earliestFluxQuery);
            log.info("房间当天用电最晚数据InfluxDB查询语句: {}", latestFluxQuery);

            List<InfluxMqttPlug> earliestTables = influxDBClient.getQueryApi()
                    .query(earliestFluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            List<InfluxMqttPlug> latestTables = influxDBClient.getQueryApi()
                    .query(latestFluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (!earliestTables.isEmpty() && !latestTables.isEmpty()) {
                InfluxMqttPlug earliestData = earliestTables.get(0);
                InfluxMqttPlug latestData = latestTables.get(0);

                RoomElectricity result = new RoomElectricity();

                // 计算当天用电量（最晚值 - 最早值）
                if (earliestData.getTotal() != null && latestData.getTotal() != null) {
                    double todayConsumption = latestData.getTotal() - earliestData.getTotal();
                    result.setTotal(Math.max(0, formatDouble(todayConsumption))); // 确保不为负数
                }

                // 使用最晚时间点的电压和电流数据
                result.setVoltage(latestData.getVoltage());
                result.setCurrent(latestData.getCurrent());

                return Result.success(result);
            } else if (!latestTables.isEmpty()) {
                // 如果只有最晚数据，返回该数据（向后兼容）
                InfluxMqttPlug latestData = latestTables.get(0);
                RoomElectricity result = new RoomElectricity();
                result.setTotal(formatDouble(latestData.getTotal()));
                result.setVoltage(latestData.getVoltage());
                result.setCurrent(latestData.getCurrent());
                return Result.success(result);
            }
            return Result.success();
        } catch (InfluxException e) {
            log.error("查询用电数据失败: {}", e.getMessage());
            return Result.failed("查询用电数据失败");
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
    public Result<List<RoomElectricityRankingVO>> getRoomElectricityRankingAsc(
            @Parameter(description = "时间范围: today-今天", example = "today")
            @RequestParam(defaultValue = "today") String range) {
        try {
            // 1. 获取所有房间列表
            List<Room> rooms = roomService.list();
            // 2. 构建结果列表
            List<RoomElectricityRankingVO> rankingList = new ArrayList<>();
            // 3. 只处理today情况
            if (!"today".equals(range)) {
                return Result.failed("只支持today时间范围参数");
            }
            // 4. 查询每个房间的日用电量差值
            for (Room room : rooms) {
                List<InfluxMqttPlug> dataList = calculateTodayElectricity(room.getId().toString());
                if (ObjectUtils.isNotEmpty(dataList)) {
                    RoomElectricityRankingVO vo = new RoomElectricityRankingVO();
                    vo.setRoomId(room.getId());
                    vo.setRoomCode(room.getClassroomCode());
                    vo.setRoomName(room.getClassroomCode());
                    if (dataList.size() >= 2) {
                        // 用最后一个点减去倒数第二个点
                        Double current = dataList.get(dataList.size() - 1).getTotal();
                        Double start = dataList.get(dataList.size() - 2).getTotal();
                        if (current != null && start != null) {
                            vo.setTotalElectricity(formatDouble(Math.max(0, current - start)));
                        }
                    }
                    if (vo.getTotalElectricity() != null) {
                        if (vo.getTotalElectricity() != 0.0) {
                            rankingList.add(vo);
                        }
                    }
                }
            }

            // 5. 按用电量升序排序（从小到大）
            rankingList.sort(Comparator.comparingDouble(
                    vo -> Optional.ofNullable(vo.getTotalElectricity()).orElse(0.0)
            ));

            // 6. 添加排名序号
            for (int i = 0; i < rankingList.size(); i++) {
                rankingList.get(i).setRank(i + 1);
            }

            return Result.success(rankingList);

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
            // 1. 根据时间单位确定窗口大小（用于聚合计算）
            String windowSize = getWindowSizeByTimeUnit(timeUnit);

            // 2. 根据传入的参数查询设备列表
            List<Device> deviceList;
            LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();

            // 添加查询条件
            if (StringUtils.isNotBlank(deviceCode)) {
                queryWrapper.eq(Device::getDeviceCode, deviceCode);
            }
            if (StringUtils.isNotBlank(roomId)) {
                queryWrapper.eq(Device::getDeviceRoom, roomId);
            }
            if (StringUtils.isNotBlank(deviceTypeId)) {
                queryWrapper.eq(Device::getDeviceTypeId, deviceTypeId);
            }

            deviceList = deviceService.list(queryWrapper);

            List<InfluxMqttPlugVO> result;
            if (deviceList.size() > 1) {
                // 多设备数据累加
                result = queryMultipleDevices(deviceList, timeAmount, timeUnit, windowSize, roomId);
            } else if (deviceList.size() == 1) {
                // 单设备查询
                result = querySingleDevice(
                        deviceList.get(0).getDeviceCode(),
                        timeAmount,
                        timeUnit,
                        windowSize,
                        roomId,
                        deviceTypeId
                );
            } else {
                // 没有找到设备
                return Result.success(new ArrayList<>());
            }

            return Result.success(result);

        } catch (InfluxException e) {
            log.error("InfluxDB查询异常", e);
            return Result.failed("数据查询失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("接口处理异常", e);
            return Result.failed("系统异常");
        }
    }

    /**
     * 查询单个设备的数据
     */
    private List<InfluxMqttPlugVO> querySingleDevice(String deviceCode, Long timeAmount, String timeUnit,
                                                     String windowSize, String roomId, String deviceTypeId) {
        try {
            String range = getRange(timeAmount, timeUnit);
            // 构建查询 - 查询所有需要的字段
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .range(range, true)
                    .measurement("device")
                    .fields("Total", "Voltage", "Current")
                    .pivot()
                    .fill()
                    .sort("_time", InfluxQueryBuilder.SORT_ASC);

            // 添加过滤条件
            if (StringUtils.isNotBlank(deviceCode)) {
                builder.tag("deviceCode", deviceCode);
            }
            if (StringUtils.isNotBlank(roomId)) {
                builder.tag("roomId", roomId);
            }
            if (StringUtils.isNotBlank(deviceTypeId)) {
                builder.tag("deviceType", deviceTypeId);
            }
            // 应用窗口聚合
            builder.window(windowSize, "last");

            String fluxQuery = builder.build();
            log.info("单设备数据查询语句: {}", fluxQuery);

            // 查询原始数据
            List<InfluxMqttPlug> dataList = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);
            // 转换为VO对象
            return convertToVOWithDifferences(dataList, timeUnit);

        } catch (Exception e) {
            log.error("查询单设备数据失败", e);
            // 发生异常时返回空的结果而不是null
            InfluxMqttPlugVO emptyVO = new InfluxMqttPlugVO();
            emptyVO.setTime(new ArrayList<>());
            emptyVO.setValue(new ArrayList<>());
            return List.of(emptyVO);
        }
    }

    /**
     * 查询多个设备的数据并合并
     */
    private List<InfluxMqttPlugVO> queryMultipleDevices(List<Device> deviceList, Long timeAmount, String timeUnit,
                                                        String windowSize, String roomId) {
        // 初始化总值列表，用于累加所有设备的数据
        List<Double> totalValues = new ArrayList<>();
        List<String> times = new ArrayList<>();

        // 遍历所有设备，获取并累加数据
        boolean hasData = false;
        for (Device device : deviceList) {
            List<InfluxMqttPlugVO> deviceDataList = querySingleDevice(device.getDeviceCode(), timeAmount, timeUnit, windowSize, roomId, null);

            if (!deviceDataList.isEmpty() && deviceDataList.get(0) != null) {
                InfluxMqttPlugVO deviceData = deviceDataList.get(0);

                // 检查是否有有效数据
                if (deviceData.getTime() != null && deviceData.getValue() != null &&
                        !deviceData.getTime().isEmpty() && !deviceData.getValue().isEmpty()) {

                    if (!hasData) {
                        // 第一个设备的数据作为初始值
                        times.addAll(deviceData.getTime());
                        totalValues.addAll(deviceData.getValue());
                        hasData = true;
                    } else {
                        // 确保两个列表长度相同后再累加
                        List<Double> deviceValues = deviceData.getValue();
                        int minSize = Math.min(totalValues.size(), deviceValues.size());
                        for (int i = 0; i < minSize; i++) {
                            Double v1 = totalValues.get(i);
                            Double v2 = deviceValues.get(i);
                            // 处理null值
                            if (v1 == null) v1 = 0.0;
                            if (v2 == null) v2 = 0.0;
                            totalValues.set(i, v1 + v2);
                        }
                    }
                }
            }
        }

        // 创建结果对象
        InfluxMqttPlugVO result = new InfluxMqttPlugVO();
        result.setTime(times);
        result.setValue(totalValues);

        return Arrays.asList(result);
    }


    private String getRange(Long timeAmount, String timeUnit) {
        // timeAmount时间范围值->1,2,3  timeUnit时间单位d,w,m,y
        switch (timeUnit) {
            case "h" -> {
                long range = timeAmount * 60L + 1;
                return range + "m";
            }
            case "d" -> {
                long range = timeAmount * 24L + 1;
                return range + "h";
            }
            case "w" -> {
                long range = timeAmount * 7L + 1;
                return range + "d";
            }
            case "mo" -> {
                long range = timeAmount * 30L + 1;
                return range + "d";
            }
            case "y" -> {
                long range = timeAmount * 12L + 1;
                return range + "mo";
            }
        }
        return "1d";
    }

    /**
     * 将原始数据转换为VO对象并计算用电量差值
     */
    private List<InfluxMqttPlugVO> convertToVOWithDifferences(List<InfluxMqttPlug> dataList, String timeUnit) {
        // 初始化结果对象
        InfluxMqttPlugVO resultVO = new InfluxMqttPlugVO();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        // 处理空数据情况
        if (dataList == null || dataList.isEmpty()) {
            resultVO.setTime(times);
            resultVO.setValue(values);
            return Arrays.asList(resultVO);
        }

        // 按时间排序
        dataList.sort(Comparator.comparing(InfluxMqttPlug::getTime));

        // 如果数据少于2条，无法计算差值
        if (dataList.size() < 2) {
            // 即使只有一条数据，也返回空的时间和值列表，而不是null
            resultVO.setTime(times);
            resultVO.setValue(values);
            return Arrays.asList(resultVO);
        }

        switch (timeUnit) {
            case "h":
                for (int i = 1; i < dataList.size() - 1; i++) {
                    times.add(formatTime(dataList.get(i).getTime(), timeUnit));
                }
                // 处理值数据：计算相邻数据点的差值
                for (int i = 0; i < dataList.size() - 2; i++) {
                    InfluxMqttPlug currentData = dataList.get(i);
                    InfluxMqttPlug nextData = dataList.get(i + 1);
                    if (i == 62) {
                        Double lastTotal = formatDouble(getTotalFromData(dataList, dataList.size() - 1));
                        Double secondLastTotal = formatDouble(getTotalFromData(dataList, dataList.size() - 2));
                        values.add(electricityCalculationService.calculateDifference(lastTotal, secondLastTotal));
                    } else {
                        Double currentTotal = formatDouble(getTotalFromData(dataList, i));
                        Double nextTotal = formatDouble(getTotalFromData(dataList, i + 1));
                        values.add(electricityCalculationService.calculateDifference(nextTotal, currentTotal));
                    }
                }
                break;

            case "d":
                for (int i = 1; i < dataList.size() - 1; i++) {
                    times.add(formatTime(dataList.get(i).getTime(), timeUnit));
                }
                // 处理值数据：计算相邻数据点的差值
                for (int i = 0; i < dataList.size() - 2; i++) {
                    InfluxMqttPlug currentData = dataList.get(i);
                    InfluxMqttPlug nextData = dataList.get(i + 1);
                    if (i == 23) {
                        Double lastTotal = formatDouble(getTotalFromData(dataList, dataList.size() - 1));
                        Double secondLastTotal = formatDouble(getTotalFromData(dataList, dataList.size() - 2));
                        values.add(electricityCalculationService.calculateDifference(lastTotal, secondLastTotal));
                    } else {
                        Double currentTotal = formatDouble(getTotalFromData(dataList, i));
                        Double nextTotal = formatDouble(getTotalFromData(dataList, i + 1));
                        values.add(electricityCalculationService.calculateDifference(nextTotal, currentTotal));
                    }
                }
                break;

            case "w":
                // 处理时间数据（从第二个数据点开始）
                for (int i = 1; i < dataList.size() - 1; i++) {
                    times.add(formatTime(dataList.get(i).getTime(), timeUnit));
                }

                // 处理值数据：计算相邻数据点的差值
                for (int i = 0; i < dataList.size() - 2; i++) {
                    if (i == 6) {
                        Double lastTotal = formatDouble(getTotalFromData(dataList, dataList.size() - 1));
                        Double secondLastTotal = formatDouble(getTotalFromData(dataList, dataList.size() - 2));
                        values.add(electricityCalculationService.calculateDifference(lastTotal, secondLastTotal));
                    } else {
                        Double currentTotal = formatDouble(getTotalFromData(dataList, i));
                        Double nextTotal = formatDouble(getTotalFromData(dataList, i + 1));
                        values.add(electricityCalculationService.calculateDifference(nextTotal, currentTotal));
                    }
                }
                break;

            case "mo":
                for (int i = 1; i < dataList.size() - 1; i++) {
                    times.add(formatTime(dataList.get(i).getTime(), timeUnit));
                }
                // 处理值数据：计算相邻数据点的差值
                for (int i = 0; i < dataList.size() - 2; i++) {
                    if (i == 29) {
                        Double lastTotal = formatDouble(getTotalFromData(dataList, dataList.size() - 1));
                        Double secondLastTotal = formatDouble(getTotalFromData(dataList, dataList.size() - 2));
                        values.add(electricityCalculationService.calculateDifference(lastTotal, secondLastTotal));
                    } else {
                        Double currentTotal = formatDouble(getTotalFromData(dataList, i));
                        Double nextTotal = formatDouble(getTotalFromData(dataList, i + 1));
                        values.add(electricityCalculationService.calculateDifference(nextTotal, currentTotal));
                    }
                }
                break;

            case "y":
                for (int i = 1; i < dataList.size() - 1; i++) {
                    //防止脏数据 新增前查询是否该日期已存在
                    String time = formatTime(dataList.get(i).getTime(), timeUnit);
                    if (times.contains(time)) {
                        continue;
                    }
                    times.add(time);
                }
                // 处理值数据：计算相邻数据点的差值
                for (int i = 0; i < dataList.size() - 2; i++) {
                    if (i == 11) {
                        Double last = formatDouble(getTotalFromData(dataList, dataList.size() - 1));
                        Double pre = formatDouble(getTotalFromData(dataList, dataList.size() - 3));
                        values.add(electricityCalculationService.calculateDifference(last, pre));
                    } else {
                        Double currentTotal = formatDouble(getTotalFromData(dataList, i));
                        Double nextTotal = formatDouble(getTotalFromData(dataList, i + 1));
                        values.add(electricityCalculationService.calculateDifference(nextTotal, currentTotal));
                    }
                }
                break;

            default:
                throw new IllegalArgumentException("暂不支持时间单位" + timeUnit);
        }

        resultVO.setTime(times);
        resultVO.setValue(values);

        return Arrays.asList(resultVO);
    }

    /**
     * 根据时间单位获取对应的窗口大小
     */
    private String getWindowSizeByTimeUnit(String timeUnit) {
        return switch (timeUnit) {
            case "y" -> "1mo";    // 按年查询：每月一个窗口
            case "mo" -> "1d";    // 按月查询：每天一个窗口
            case "w" -> "1d";     // 按周查询：每天一个窗口
            case "d" -> "1h";     // 按天查询：每小时一个窗口
            case "h" -> "1m";     // 按小时查询：每分钟一个窗口
            case "m" -> "1s";     // 按分钟查询：每秒一个窗口
            default -> "1h";
        };
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

    /**
     * 计算今日用电量
     */
    private List<InfluxMqttPlug> calculateTodayElectricity(String roomId) {
        try {
            Instant now = Instant.now();
            Instant startOfDay = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();

            // 查询今日0点至今的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .pivot()
                    .window("1d", "last")
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .range(startOfDay, now);

            if (StringUtils.isNotBlank(roomId)) {
                List<String> roomIdList = Arrays.stream(roomId.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .toList();

                if (!roomIdList.isEmpty()) {
                    String roomFilter = roomIdList.stream()
                            .map(id -> String.format("r.roomId == \"%s\"", id))
                            .collect(Collectors.joining(" or "));
                    builder.filter("(" + roomFilter + ")");
                }
            }

            String fluxQuery = builder.build();
            log.info("查询今日用电查询语句: {}", fluxQuery);

            return influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * 安全获取数据点的Total值
     * @param dataList 数据列表
     * @param index 索引
     * @return Total值，如果数据不存在或为null则返回0.0
     */
    private Double getTotalFromData(List<InfluxMqttPlug> dataList, int index) {
        if (dataList == null || index < 0 || index >= dataList.size()) {
            return 0.0;
        }

        InfluxMqttPlug data = dataList.get(index);
        if (data == null || data.getTotal() == null) {
            return 0.0;
        }

        return data.getTotal();
    }

}
