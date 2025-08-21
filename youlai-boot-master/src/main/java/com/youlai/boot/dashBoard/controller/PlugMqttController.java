package com.youlai.boot.dashBoard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;
import com.youlai.boot.categoryDeviceRelationship.service.CategoryDeviceRelationshipService;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.InfluxQueryBuilder;
import com.youlai.boot.common.util.MathUtils;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.dashBoard.model.vo.CategoryElectricityVO;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import com.youlai.boot.device.model.vo.InfluxMqttPlugVO;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.system.model.entity.Config;
import com.youlai.boot.system.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 *@Author: way
 *@CreateTime: 2025-08-18  16:57
 *@Description: TODO
 */
@Tag(name = "demodemodemo")
@RestController
@RequestMapping("/api/v1/demodemo")
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PlugMqttController {
    private final InfluxDBClient influxDBClient;
    private final ConfigService configService;
    private final CategoryService categoryService;
    private final CategoryDeviceRelationshipService categoryDeviceRelationshipService;

    private final DeviceService deviceService;

    private final InfluxDBProperties influxDBProperties;

    @Operation(summary = "查询计量插座数据图数据")
    @GetMapping("/demodemo")
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

            // 2. 处理多设备聚合场景
            List<Device> deviceList = new ArrayList<>();
            if (StringUtils.isBlank(deviceCode) && StringUtils.isBlank(roomId) && StringUtils.isNotBlank(deviceTypeId)) {
                deviceList = deviceService.list(new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceTypeId, deviceTypeId));
            }

            List<InfluxMqttPlugVO> result;
            if (!deviceList.isEmpty()) {
                // 多设备数据累加
                result = queryMultipleDevices(deviceList, timeAmount, timeUnit, windowSize, roomId);
            } else {
                // 单设备查询
                result = querySingleDevice(deviceCode, timeAmount, timeUnit, windowSize, roomId, deviceTypeId);
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
//                    .timeShift("8h")
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
            return new ArrayList<>();
        }
    }

    /**
     * 查询多个设备的数据并合并
     */
    private List<InfluxMqttPlugVO> queryMultipleDevices(List<Device> deviceList, Long timeAmount, String timeUnit,
                                                        String windowSize, String roomId) {
        try {
            // 存储所有设备的数据，按时间分组
            Map<Instant, List<InfluxMqttPlug>> timeDataMap = new TreeMap<>();
            String range = getRange(timeAmount, timeUnit);
            for (Device device : deviceList) {
                // 为每个设备单独构建查询
                InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                        .bucket(influxDBProperties.getBucket())
                        .range(range, true)  // 多查一天的数据用于计算差值
                        .measurement("device")
                        .fields("Total", "Voltage", "Current")
                        .tag("deviceCode", device.getDeviceCode())
                        .timeShift("8h")
                        .pivot()
                        .fill()
                        .sort("_time", InfluxQueryBuilder.SORT_ASC);

                // 如果指定了roomId，也添加到过滤条件中
                if (StringUtils.isNotBlank(roomId)) {
                    builder.tag("roomId", roomId);
                }

                // 应用窗口聚合
                builder.window(windowSize, "last");

                String fluxQuery = builder.build();
                log.info("设备[{}]数据查询语句: {}", device.getDeviceCode(), fluxQuery);

                // 查询单个设备的数据
                List<InfluxMqttPlug> deviceDataList = influxDBClient.getQueryApi()
                        .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);
                // 按时间分组存储数据
                for (InfluxMqttPlug data : deviceDataList) {
                    timeDataMap.computeIfAbsent(data.getTime(), k -> new ArrayList<>()).add(data);
                }
            }

            // 合并同一时间点的多个设备数据
            List<InfluxMqttPlug> mergedDataList = new ArrayList<>();
            for (Map.Entry<Instant, List<InfluxMqttPlug>> entry : timeDataMap.entrySet()) {
                Instant time = entry.getKey();
                List<InfluxMqttPlug> dataList = entry.getValue();

                // 合并同一时间点的所有设备数据
                InfluxMqttPlug mergedData = new InfluxMqttPlug();
                mergedData.setTime(time);

                double totalSum = 0;
                double voltageSum = 0;
                double currentSum = 0;
                int voltageCount = 0;
                int currentCount = 0;

                for (InfluxMqttPlug data : dataList) {
                    if (data.getTotal() != null) {
                        totalSum += data.getTotal();
                    }
                    if (data.getVoltage() != null) {
                        voltageSum += data.getVoltage();
                        voltageCount++;
                    }
                    if (data.getCurrent() != null) {
                        currentSum += data.getCurrent();
                        currentCount++;
                    }
                }

                mergedData.setTotal(totalSum);
                mergedData.setVoltage(voltageCount > 0 ? voltageSum / voltageCount : null);
                mergedData.setCurrent(currentCount > 0 ? currentSum / currentCount : null);

                mergedDataList.add(mergedData);
            }

            // 转换为VO对象
            return convertToVOWithDifferences(mergedDataList, timeUnit);

        } catch (Exception e) {
            log.error("查询多设备数据失败", e);
            return new ArrayList<>();
        }
    }

    private String getRange(Long timeAmount, String timeUnit) {
        // timeAmount时间范围值->1,2,3  timeUnit时间单位d,w,m,y
        switch (timeUnit) {
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
                return range + "y";
            }
        }
        return "1d";
    }

//    /**
//     * 将原始数据转换为VO对象并计算用电量差值
//     */
//    private List<InfluxMqttPlugVO> convertToVOWithDifferences(List<InfluxMqttPlug> dataList, String timeUnit) {
//        if (dataList == null || dataList.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        InfluxMqttPlugVO resultVO = new InfluxMqttPlugVO();
//        List<String> times = new ArrayList<>();
//        List<Double> values = new ArrayList<>();
//
//        // 按时间排序
//        dataList.sort(Comparator.comparing(InfluxMqttPlug::getTime));
//
//        // 如果数据少于2条，无法计算差值
//        if (dataList.size() < 2) {
//            List<InfluxMqttPlugVO> result = new ArrayList<>();
//            result.add(resultVO);
//            return result;
//        }
//
//        // 去掉倒数第二条数据
//        List<InfluxMqttPlug> processedDataList = new ArrayList<>();
//        for (int i = 0; i < dataList.size(); i++) {
//            // 跳过倒数第二条数据
//            if (i != dataList.size() - 2) {
//                processedDataList.add(dataList.get(i));
//            }
//        }
//
//        // 处理时间数据：索引1,2,3,4,5,6,7 (共7个时间点)
//        for (int i = 1; i < processedDataList.size(); i++) {
//            times.add(formatTime(processedDataList.get(i).getTime(), timeUnit));
//        }
//
//        // 处理值数据：1-0, 2-1, 3-2, 4-3, 5-4, 6-5, 7-6, 8-7 (共8个差值，但实际只需要7个)
//        for (int i = 0; i < processedDataList.size() - 1; i++) {
//            InfluxMqttPlug currentData = processedDataList.get(i);
//            InfluxMqttPlug nextData = processedDataList.get(i + 1);
//
//            if (currentData.getTotal() != null && nextData.getTotal() != null) {
//                double difference = Math.max(0, MathUtils.formatDouble(
//                        nextData.getTotal() - currentData.getTotal()));
//                values.add(difference);
//            } else {
//                values.add(0.0);
//            }
//        }
//
//        resultVO.setTime(times);
//        resultVO.setValue(values);
//
//        List<InfluxMqttPlugVO> result = new ArrayList<>();
//        result.add(resultVO);
//        return result;
//    }

    /**
     * 将原始数据转换为VO对象并计算用电量差值
     */
    private List<InfluxMqttPlugVO> convertToVOWithDifferences(List<InfluxMqttPlug> dataList, String timeUnit) {
        if (dataList == null || dataList.isEmpty()) {
            return new ArrayList<>();
        }

        InfluxMqttPlugVO resultVO = new InfluxMqttPlugVO();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        // 按时间排序
        dataList.sort(Comparator.comparing(InfluxMqttPlug::getTime));

        // 如果数据少于2条，无法计算差值
        if (dataList.size() < 2) {
            List<InfluxMqttPlugVO> result = new ArrayList<>();
            result.add(resultVO);
            return result;
        }

        switch (timeUnit) {
            case "d":
                // 天级别：计算当天用电量 = 最新值 - 当天0点值
                times.add(formatTime(dataList.get(dataList.size() - 1).getTime(), timeUnit));
                if (dataList.get(0).getTotal() != null && dataList.get(dataList.size() - 1).getTotal() != null) {
                    double difference = Math.max(0, MathUtils.formatDouble(
                            dataList.get(dataList.size() - 1).getTotal() - dataList.get(0).getTotal()));
                    values.add(difference);
                } else {
                    values.add(0.0);
                }
                break;

            case "w":
                // 周级别：计算每天的用电量差值
                // 第一天：第二个数据点 - 第一个数据点
                // 中间天：当天数据点 - 前一天数据点
                // 最后一天：最后数据点 - 倒数第二数据点

                // 处理时间数据（从第二个数据点开始）
                for (int i = 1; i < dataList.size(); i++) {
                    if (i != dataList.size() - 1) {
                        times.add(formatTime(dataList.get(i).getTime(), timeUnit));
                    }
                }

                // 处理值数据：计算相邻数据点的差值
                for (int i = 0; i < dataList.size() - 1; i++) {
                    InfluxMqttPlug currentData = dataList.get(i);
                    InfluxMqttPlug nextData = dataList.get(i + 1);

                    if (currentData.getTotal() != null && nextData.getTotal() != null) {
                        double difference = Math.max(0, MathUtils.formatDouble(
                                nextData.getTotal() - currentData.getTotal()));
                        values.add(difference);
                    } else {
                        values.add(0.0);
                    }
                }
                break;

            default:
                // 其他时间单位保持原有逻辑
                // 处理时间数据（从第二个数据点开始）
                for (int i = 1; i < dataList.size(); i++) {
                    times.add(formatTime(dataList.get(i).getTime(), timeUnit));
                }

                // 处理值数据：计算相邻数据点的差值
                for (int i = 0; i < dataList.size() - 1; i++) {
                    InfluxMqttPlug currentData = dataList.get(i);
                    InfluxMqttPlug nextData = dataList.get(i + 1);

                    if (currentData.getTotal() != null && nextData.getTotal() != null) {
                        double difference = Math.max(0, MathUtils.formatDouble(
                                nextData.getTotal() - currentData.getTotal()));
                        values.add(difference);
                    } else {
                        values.add(0.0);
                    }
                }
                break;
        }

        resultVO.setTime(times);
        resultVO.setValue(values);

        List<InfluxMqttPlugVO> result = new ArrayList<>();
        result.add(resultVO);
        return result;
    }


    /**
     * 根据时间单位获取对应的窗口大小
     */
    private String getWindowSizeByTimeUnit(String timeUnit) {
        return switch (timeUnit) {
            case "y" -> "1mo";    // 按年查询：每月一个窗口device/master
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
        ZoneId utcZone = ZoneId.of("UTC");
        return switch (timeUnit) {
            case "y" -> time.atZone(utcZone).getYear() + "/" +
                    String.format("%02d", time.atZone(utcZone).getMonthValue());
            case "mo" -> String.format("%02d", time.atZone(utcZone).getMonthValue()) + "/" +
                    String.format("%02d", time.atZone(utcZone).getDayOfMonth());
            case "w" -> {
                Instant now = Instant.now();
                Instant currentMonday = now.atZone(utcZone)
                        .with(java.time.DayOfWeek.MONDAY)
                        .toLocalDate()
                        .atStartOfDay(utcZone)
                        .toInstant();
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
            case "h" -> String.format("%02d:%02d",
                    time.atZone(utcZone).getHour(),
                    time.atZone(utcZone).getMinute());
            case "m" -> String.format("%02d:%02d:%02d",
                    time.atZone(utcZone).getHour(),
                    time.atZone(utcZone).getMinute(),
                    time.atZone(utcZone).getSecond());
            default -> time.atZone(utcZone).format(DateTimeFormatter.ISO_LOCAL_TIME);
        };
    }

    @Operation(summary = "查询配置分类用电量")
    @GetMapping("/category/demodemo")
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

        // 3. 获取时间模板（从第一个分类的第一个设备获取）
        if (!categoriesAll.isEmpty()) {
            List<CategoryDeviceRelationship> relationships =
                    categoryDeviceRelationshipService.listByCategoryId(categoriesAll.get(0).getId());
            if (!relationships.isEmpty()) {
                Device device = deviceService.getById(relationships.get(0).getDeviceId());
                // 查询第一个设备的数据来获取时间标签
                List<InfluxMqttPlug> sampleDataList = queryDeviceRawData(device.getDeviceCode(), 1L, "w", "1d");
                if (!sampleDataList.isEmpty()) {
                    // 使用convertToVOWithDifferences方法处理数据以获取正确的时间标签
                    List<InfluxMqttPlugVO> sampleVOList = convertToVOWithDifferences(sampleDataList, "w");
                    if (!sampleVOList.isEmpty()) {
                        result.setTimes(sampleVOList.get(0).getTime());
                    }
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

    /**
     * 查询设备原始数据（不进行差值计算）
     */
    private List<InfluxMqttPlug> queryDeviceRawData(String deviceCode, Long timeAmount, String timeUnit, String windowSize) {
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
            // 应用窗口聚合
            builder.window(windowSize, "last");

            String fluxQuery = builder.build();
            log.info("设备原始数据查询语句: {}", fluxQuery);

            // 查询原始数据
            return influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

        } catch (Exception e) {
            log.error("查询设备原始数据失败", e);
            return new ArrayList<>();
        }
    }

}