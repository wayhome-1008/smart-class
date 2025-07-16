package com.youlai.boot.dashBoard.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
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
    private final CategoryDeviceRelationshipService categoryDeviceRelationshipService;
    private final RedisTemplate<String, Object> redisTemplate;

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
//        // 3. 获取时间基准（从任意一个分类查询获取即可）
//        List<String> timeTemplate = getTimeTemplate();
//        result.setTimes(timeTemplate);
        // 3. 获取时间模板（从第一个分类的第一个设备获取）
        if (!categoriesAll.isEmpty()) {
            List<CategoryDeviceRelationship> relationships =
                    categoryDeviceRelationshipService.listByCategoryId(categoriesAll.get(0).getId());
            if (!relationships.isEmpty()) {
                Device device = deviceService.getById(relationships.get(0).getDeviceId());
                List<InfluxMqttPlug> sampleTables = influxDBClient.getQueryApi()
                        .query(InfluxQueryBuilder.newBuilder()
                                        .bucket(influxDBProperties.getBucket())
                                        .range(7, "d")
                                        .measurement("device")
                                        .fields("Total")
                                        .tag("deviceCode", device.getDeviceCode())
                                        .window("1d", "last")
                                        .build(),
                                influxDBProperties.getOrg(),
                                InfluxMqttPlug.class);
                // 按时间排序并提取时间字符串
                List<String> times = sampleTables.stream()
                        .sorted(Comparator.comparing(InfluxMqttPlug::getTime)) // 再次确保排序
                        .map(table -> formatTime(table.getTime(), "w"))
                        .collect(Collectors.toList());
                result.setTimes(times);
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

                    List<Double> totalValues = null;
                    for (Device masterDevice : masterDevices) {
                        List<Double> deviceValues = queryDeviceWeeklyData(masterDevice.getDeviceCode());
                        if (totalValues == null) {
                            totalValues = new ArrayList<>(deviceValues);
                        } else {
                            for (int i = 0; i < deviceValues.size(); i++) {
                                if (i < totalValues.size()) {
                                    totalValues.set(i, totalValues.get(i) + deviceValues.get(i));
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

    // 查询设备一周数据
    private List<Double> queryDeviceWeeklyData(String deviceCode) {
        InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                .bucket(influxDBProperties.getBucket())
                .range(7, "d")
                .measurement("device")
                .fields("Total")
                .tag("deviceCode", deviceCode)
                .pivot()
                .fill()
                .sort("_time", InfluxQueryBuilder.SORT_ASC)
                .timeShift("8h")
                .window("1d", "last");

        List<InfluxMqttPlug> tables = influxDBClient.getQueryApi()
                .query(builder.build(), influxDBProperties.getOrg(), InfluxMqttPlug.class);

        return tables.stream()
                .map(InfluxMqttPlug::getTotal)
                .map(MathUtils::formatDouble)
                .collect(Collectors.toList());
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

    @Operation(summary = "房间当天用电信息(仅显示总用电、功率、电压)")
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
            log.info("房间当天用电InfluxDB查询语句: {}", fluxQuery);

            List<InfluxMqttPlug> tables = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);         //根据influxdb传来的数据把度数算上
            if (!tables.isEmpty()) {
                InfluxMqttPlug latestData = tables.get(0);
                RoomElectricity result = new RoomElectricity();
                result.setTotal(formatDouble(latestData.getTotal()));
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
            List<Device> deviceList = new ArrayList<>();
            if (StringUtils.isBlank(deviceCode) && StringUtils.isBlank(roomId) && StringUtils.isNotBlank(deviceTypeId)) {
                //说明查所有
                deviceList = deviceService.list(new LambdaQueryWrapper<Device>().eq(Device::getDeviceTypeId, deviceTypeId));
            }
            List<InfluxMqttPlug> tables;
            List<InfluxMqttPlugVO> influxMqttPlugVOS = new ArrayList<>();
            String fluxQuery = "";
            if (ObjectUtils.isNotEmpty(deviceList)) {
                for (Device device : deviceList) {
                    builder.tag("deviceCode", device.getDeviceCode());
                    fluxQuery = builder.build();
                    log.info("查询计量插座数据图数据InfluxDB查询语句: {}", fluxQuery);
                    List<InfluxMqttPlug> query = influxDBClient.getQueryApi()
                            .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);
                    influxMqttPlugVOS.addAll(makeInfluxMqttPlugVOListAll(query, timeUnit));
                }
                List<Double> resultValue = new ArrayList<>();
                for (InfluxMqttPlugVO influxMqttPlugVO : influxMqttPlugVOS) {
                    List<Double> value = influxMqttPlugVO.getValue();
                    if (ObjectUtils.isEmpty(resultValue)) {
                        resultValue = new ArrayList<>(value); // 创建新列表避免引用问题
                    } else {
                        // 确保两个列表长度相同
                        int minSize = Math.min(resultValue.size(), value.size());
                        for (int i = 0; i < minSize; i++) {
                            Double v1 = resultValue.get(i);
                            Double v2 = value.get(i);
                            // 处理null值
                            if (v1 == null) v1 = 0.0;
                            if (v2 == null) v2 = 0.0;
                            resultValue.set(i, v1 + v2);
                        }
                    }
                }
                List<InfluxMqttPlugVO> influxMqttPlugVOS1 = new ArrayList<>();
                InfluxMqttPlugVO influxMqttPlugVO = new InfluxMqttPlugVO();
                influxMqttPlugVO.setTime(influxMqttPlugVOS.get(0).getTime());
                influxMqttPlugVO.setValue(resultValue);
                influxMqttPlugVOS1.add(influxMqttPlugVO);
                return Result.success(influxMqttPlugVOS1);
            } else {
                tables = influxDBClient.getQueryApi()
                        .query(builder.build(), influxDBProperties.getOrg(), InfluxMqttPlug.class);
                log.info("查询计量插座数据图InfluxDB查询语句: {}", fluxQuery);
                // 转换结果并返回
                return Result.success(makeInfluxMqttPlugVOList(tables, timeUnit));
            }
            //根据influxdb传来的数据把度数算上

        } catch (InfluxException e) {
            System.err.println("error：" + e.getMessage());
        }
        return Result.failed();
    }

    private List<InfluxMqttPlugVO> makeInfluxMqttPlugVOListAll(List<InfluxMqttPlug> tables, String timeUnit) {
        List<InfluxMqttPlugVO> result = new ArrayList<>();
        InfluxMqttPlugVO vo = new InfluxMqttPlugVO();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (InfluxMqttPlug table : tables) {
            // 格式化时间（根据你的需求选择格式）
            String formattedTime = formatTime(table.getTime(), timeUnit);
            times.add(formattedTime);
            values.add(formatDouble(table.getTotal()));
        }
        vo.setTime(times);
        vo.setValue(values);
        result.add(vo);
        return result;
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
            @Parameter(description = "时间范围: today-今天/month-本月/year-本年", example = "today")
            @RequestParam(defaultValue = "today") String range) {
        try {
            // 1. 获取所有房间列表
            List<Room> rooms = roomService.list();

            // 2. 构建结果列表
            List<RoomElectricityRankingVO> rankingList = new ArrayList<>();

            // 3. 查询每个房间的总用电量
            for (Room room : rooms) {
                // 为每个房间创建新查询构建器
                InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                        .bucket(influxDBProperties.getBucket())
                        .measurement("device")
                        .fields("Total")
                        .sum()
                        .timeShift("8h");

                // 设置时间范围
                switch (range) {
                    case "today":
                        builder.today();
                        break;
                    case "month":
                        builder.currentMonth();
                        break;
                    case "year":
                        builder.currentYear();
                        break;
                    default:
                        return Result.failed("无效的时间范围参数");
                }

                // 添加房间ID过滤和聚合
                builder.tag("roomId", String.valueOf(room.getId()))
                        .sum();  // 使用专门的sum方法

                String fluxQuery = builder.build();
                log.info("InfluxDB查询语句: {}", fluxQuery);
                List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery);
                Double total = 0d;
                for (FluxTable table : tables) {
                    for (FluxRecord record : table.getRecords()) {
                        total = (Double) record.getValueByKey("_value"); // 或 "Total"
                        log.info("获取到的Total值: {}", total);
                    }
                }
                if (!tables.isEmpty()) {
                    RoomElectricityRankingVO vo = new RoomElectricityRankingVO();
                    vo.setRoomId(room.getId());
                    vo.setRoomCode(room.getClassroomCode());
                    vo.setRoomName(room.getClassroomCode());
                    vo.setTotalElectricity(formatDouble(total));
                    rankingList.add(vo);
                }
            }

            // 4. 按用电量升序排序（从小到大）
// 安全处理null值的排序方式
            rankingList.sort(Comparator.comparingDouble(
                    vo -> Optional.ofNullable(vo.getTotalElectricity()).orElse(0.0)
            ));

            // 5. 添加排名序号
            for (int i = 0; i < rankingList.size(); i++) {
                rankingList.get(i).setRank(i + 1);
            }

            return Result.success(rankingList);

        } catch (InfluxException e) {
            log.error("查询房间用电量排名失败: {}", e.getMessage());
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
            //显示月份
            case "y" -> time.atZone(utcZone).getYear() + "/" +
                    String.format("%02d", time.atZone(utcZone).getMonthValue());
            //显示日期
            case "mo" -> String.format("%02d", time.atZone(utcZone).getMonthValue()) + "/" +
                    String.format("%02d", time.atZone(utcZone).getDayOfMonth());
            case "w" -> { // 显示中文星期，区分本周和上周
                // 获取当前周一的日期
                Instant now = Instant.now();
                Instant currentMonday = now.atZone(utcZone)
                        .with(java.time.DayOfWeek.MONDAY)
                        .toLocalDate()
                        .atStartOfDay(utcZone)
                        .toInstant();

                // 判断记录时间是本周还是上周
                String prefix = time.isBefore(currentMonday) ? "上" : "";

                int dayOfWeek = time.atZone(utcZone).getDayOfWeek().getValue();
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
            case "d" -> String.format("%02d", time.atZone(utcZone).getHour()) + ":00";
            case "h" -> String.format("%02d:%02d", time.atZone(utcZone).getHour(), time.atZone(utcZone).getMinute());
            case "HH:mm:ss" -> String.format("%02d:%02d:%02d",
                    time.atZone(utcZone).getHour(),
                    time.atZone(utcZone).getMinute(),
                    time.atZone(utcZone).getSecond());
            //显示年月日时分秒
            case "yyyy-MM-dd HH:mm:ss" -> String.format("%04d-%02d-%02d %02d:%02d:%02d",
                    time.atZone(utcZone).getYear(),
                    time.atZone(utcZone).getMonthValue(),
                    time.atZone(utcZone).getDayOfMonth(),
                    time.atZone(utcZone).getHour(),
                    time.atZone(utcZone).getMinute(),
                    time.atZone(utcZone).getSecond());
            default -> time.atZone(utcZone).format(DateTimeFormatter.ISO_LOCAL_TIME);
        };
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

}
