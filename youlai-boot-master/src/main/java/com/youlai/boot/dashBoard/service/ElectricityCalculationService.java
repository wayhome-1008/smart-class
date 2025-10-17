package com.youlai.boot.dashBoard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.util.InfluxQueryBuilder;
import com.youlai.boot.common.util.MathUtils;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.dashBoard.model.vo.CategoryElectricityData;
import com.youlai.boot.dashBoard.model.vo.RoomsElectricityVO;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 *@Author: way
 *@CreateTime: 2025-08-26  11:46
 *@Description: 用电量计算服务类，统一处理各种用电量计算逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElectricityCalculationService {
    private final InfluxDBProperties influxDBProperties;
    private final com.influxdb.client.InfluxDBClient influxDBClient;
    private final DeviceService deviceService;
    private final RoomService roomService;
    private final CategoryService categoryService;

    /**
     * 计算设备用电量
     */
    public Double calculateDeviceElectricity(Device device, String range, String startTime, String endTime, String roomIds) {
        try {
            Double totalElectricity = switch (range) {
                case "today" -> calculateTodayElectricity(device, roomIds);
                case "yesterday" -> calculateYesterdayElectricity(device, roomIds);
                case "week" -> calculateWeekElectricity(device, roomIds);
                case "lastWeek" -> calculateLastWeekElectricity(device, roomIds);
                case "month" -> calculateMonthElectricity(device, roomIds);
                case "lastMonth" -> calculateLastMonthElectricity(device, roomIds);
                default -> calculateCustomElectricity(device, startTime, endTime, roomIds);
            };
            return MathUtils.formatDouble(totalElectricity);
        } catch (Exception e) {
            log.error("计算设备用电量失败 - 设备编码: {}, 范围: {}, 异常信息: ", device, range, e);
            return 0.0;
        }
    }

    /**
     * 根据时间单位查询用电量数据，适配CategoryElectricityVO格式
     * @param deviceList 设备对象集合
     * @param timeUnit 时间单位 (d-日, w-周, mo-月, y-年)
     * @param roomIds 房间ID列表
     * @return 包含时间和用电量值的列表，适配CategoryElectricityVO格式
     */
    public CategoryElectricityData getElectricityDataForCategory(List<Device> deviceList, String timeUnit, String roomIds) {
        return switch (timeUnit) {
            case "d" -> getDailyElectricityDataForCategory(deviceList, roomIds);
            case "w" -> getWeeklyElectricityDataForCategory(deviceList, roomIds);
            case "mo" -> getMonthlyElectricityDataForCategory(deviceList, roomIds);
            case "y" -> getYearlyElectricityDataForCategory(deviceList, roomIds);
            default -> getWeeklyElectricityDataForCategory(deviceList, roomIds);
        };
    }


    /**
     * 计算房间的用电量
     */
    public Double calculateRoomElectricity(Long roomId, String range, String startTime, String endTime, Long categoryId) {
        try {
            // 获取房间内所有主设备
            List<Device> roomDevices = deviceService.listDevicesByCategoryAndRoomId(categoryId, roomId);
            double roomTotalElectricity = 0.0;
            for (Device device : roomDevices) {
                Double deviceElectricity = calculateDeviceElectricity(device, range, startTime, endTime, roomId.toString());
                if (deviceElectricity != null) {
                    roomTotalElectricity += deviceElectricity;
                }
            }
            return MathUtils.formatDouble(roomTotalElectricity);
        } catch (Exception e) {
            log.error("计算房间用电量失败 - roomId: {}, range: {}", roomId, range, e);
            return 0.0;
        }
    }

    /**
     * 计算今日用电量
     */
    public Double calculateTodayElectricity(Device device, String roomIds) {
        try {
            Instant now = Instant.now();
            Instant startOfDay = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            //获取当天结束时间
            Instant endOfDay = LocalDate.now().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

            // 查询今日0点至今的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder().bucket(influxDBProperties.getBucket()).measurement("device").fields("Total").tag("deviceCode", device.getDeviceCode()).tag("categoryId", device.getCategoryId().toString()).pivot()
//                    .window("1d", "last")
                    .sort("_time", InfluxQueryBuilder.SORT_ASC).range(startOfDay, endOfDay);

            if (StringUtils.isNotBlank(roomIds)) {
                List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                if (!roomIdList.isEmpty()) {
                    String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                    builder.filter("(" + roomFilter + ")");
                }
            } else {
                builder.tag("roomId", device.getDeviceRoom().toString());
            }
            String fluxQuery = builder.build();
            log.info("查询今日用电量 - 设备编码: {}, 查询语句: {}", device.getDeviceCode(), fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);
            return calculateElectricityWithNullHandling(results);
        } catch (Exception e) {
            log.error("计算今日用电量失败 - 设备编码: {}", device.getDeviceCode(), e);
            return 0.0;
        }
    }

    /**
     * 计算昨日用电量
     */
    private Double calculateYesterdayElectricity(Device device, String roomIds) {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Instant startOfYesterday = yesterday.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Instant endOfYesterday = yesterday.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

            // 查询昨日0点到昨日23:59:59的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder().bucket(influxDBProperties.getBucket()).measurement("device").fields("Total").tag("deviceCode", device.getDeviceCode()).tag("categoryId", device.getCategoryId().toString()).pivot()
//                    .window("1d", "last")
                    .sort("_time", InfluxQueryBuilder.SORT_ASC).range(startOfYesterday, endOfYesterday);

            if (StringUtils.isNotBlank(roomIds)) {
                List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                if (!roomIdList.isEmpty()) {
                    String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                    builder.filter("(" + roomFilter + ")");
                }
            } else {
                builder.tag("roomId", device.getDeviceRoom().toString());
            }

            String fluxQuery = builder.build();
            log.info("查询昨日用电量 - 设备编码: {}, 查询语句: {}", device.getDeviceCode(), fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

//            if (results.size() >= 2) {
//                // 用最后一个点减去第一个点
//                Double end = results.get(results.size() - 1).getTotal();
//                Double start = results.get(0).getTotal();
//                if (end != null && start != null) {
//                    return Math.max(0, end - start);
//                }
//            }
//            return 0.0;
            return calculateElectricityWithNullHandling(results);
        } catch (Exception e) {
            log.error("计算昨日用电量失败 - 设备编码: {}", device.getDeviceCode(), e);
            return 0.0;
        }
    }

    /**
     * 计算本周用电量
     */
    private Double calculateWeekElectricity(Device device, String roomIds) {
        try {
            LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            LocalDate today = LocalDate.now();
            Instant startOfWeek = monday.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Instant endOfToday = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

            // 查询本周一0点到今天23:59:59的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder().bucket(influxDBProperties.getBucket()).measurement("device").fields("Total").tag("deviceCode", device.getDeviceCode()).tag("categoryId", device.getCategoryId().toString()).pivot().window("1d", "last").sort("_time", InfluxQueryBuilder.SORT_ASC).range(startOfWeek, endOfToday);

            if (StringUtils.isNotBlank(roomIds)) {
                List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                if (!roomIdList.isEmpty()) {
                    String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                    builder.filter("(" + roomFilter + ")");
                }
            } else {
                builder.tag("roomId", device.getDeviceRoom().toString());
            }

            String fluxQuery = builder.build();
            log.info("查询本周用电量 - 设备编码: {}, 查询语句: {}", device.getDeviceCode(), fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

//            if (results.size() >= 2) {
//                // 用最后一个点减去第一个点
//                Double end = results.get(results.size() - 1).getTotal();
//                Double start = results.get(0).getTotal();
//                if (end != null && start != null) {
//                    return Math.max(0, end - start);
//                }
//            }
//
//            return 0.0;
            return calculateElectricityWithNullHandling(results);
        } catch (Exception e) {
            log.error("计算本周用电量失败 - 设备编码: {}", device.getDeviceCode(), e);
            return 0.0;
        }
    }


    /**
     * 计算上周用电量
     */
    private Double calculateLastWeekElectricity(Device device, String roomIds) {
        try {
            LocalDate lastMonday = LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
            LocalDate thisMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            Instant startOfLastWeek = lastMonday.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Instant endOfLastWeek = thisMonday.minusDays(1).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

            // 查询上周一0点到上周日23:59:59的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder().bucket(influxDBProperties.getBucket()).measurement("device").fields("Total").tag("deviceCode", device.getDeviceCode()).tag("categoryId", device.getCategoryId().toString()).pivot().window("1d", "last").sort("_time", InfluxQueryBuilder.SORT_ASC).range(startOfLastWeek, endOfLastWeek);

            if (StringUtils.isNotBlank(roomIds)) {
                List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                if (!roomIdList.isEmpty()) {
                    String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                    builder.filter("(" + roomFilter + ")");
                }
            } else {
                builder.tag("roomId", device.getDeviceRoom().toString());
            }

            String fluxQuery = builder.build();
            log.info("查询上周用电量 - 设备编码: {}, 查询语句: {}", device.getDeviceCode(), fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

//            if (results.size() >= 2) {
//                // 用倒数第二个点减去第一个点
//                Double end = results.get(results.size() - 1).getTotal();
//                Double start = results.get(0).getTotal();
//                if (end != null && start != null) {
//                    return Math.max(0, end - start);
//                }
//            }
//
//            return 0.0;
            return calculateElectricityWithNullHandling(results);
        } catch (Exception e) {
            log.error("计算上周用电量失败 - 设备编码: {}", device.getDeviceCode(), e);
            return 0.0;
        }
    }


    /**
     * 计算本月用电量
     */
    private Double calculateMonthElectricity(Device device, String roomIds) {
        try {
            LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate today = LocalDate.now();
            Instant startOfMonth = firstDayOfMonth.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Instant endOfToday = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

            // 查询本月1号0点到今天23:59:59的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder().bucket(influxDBProperties.getBucket()).measurement("device").fields("Total").tag("deviceCode", device.getDeviceCode()).tag("categoryId", device.getCategoryId().toString()).pivot().window("1d", "last").sort("_time", InfluxQueryBuilder.SORT_ASC).range(startOfMonth, endOfToday);

            if (StringUtils.isNotBlank(roomIds)) {
                List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                if (!roomIdList.isEmpty()) {
                    String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                    builder.filter("(" + roomFilter + ")");
                }
            } else {
                builder.tag("roomId", device.getDeviceRoom().toString());
            }

            String fluxQuery = builder.build();
            log.info("查询本月用电量 - 设备编码: {}, 查询语句: {}", device.getDeviceCode(), fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

//            if (results.size() >= 2) {
//                // 用最后一个点减去第一个点
//                Double end = results.get(results.size() - 1).getTotal();
//                Double start = results.get(0).getTotal();
//                if (end != null && start != null) {
//                    return Math.max(0, end - start);
//                }
//            }
//
//            return 0.0;
            return calculateElectricityWithNullHandling(results);
        } catch (Exception e) {
            log.error("计算本月用电量失败 - 设备编码: {}", device.getDeviceCode(), e);
            return 0.0;
        }
    }

    /**
     * 计算上月用电量
     */
    private Double calculateLastMonthElectricity(Device device, String roomIds) {
        try {
            LocalDate firstDayOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            LocalDate firstDayOfThisMonth = LocalDate.now().withDayOfMonth(1);
            Instant startOfLastMonth = firstDayOfLastMonth.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Instant endOfLastMonth = firstDayOfThisMonth.minusDays(1).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

            // 查询上月1号0点到上月最后一天23:59:59的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder().bucket(influxDBProperties.getBucket()).measurement("device").fields("Total").tag("deviceCode", device.getDeviceCode()).tag("categoryId", device.getCategoryId().toString()).pivot().window("1d", "last").sort("_time", InfluxQueryBuilder.SORT_ASC).range(startOfLastMonth, endOfLastMonth);

            if (StringUtils.isNotBlank(roomIds)) {
                List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                if (!roomIdList.isEmpty()) {
                    String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                    builder.filter("(" + roomFilter + ")");
                }
            } else {
                builder.tag("roomId", device.getDeviceRoom().toString());
            }

            String fluxQuery = builder.build();
            log.info("查询上月用电量 - 设备编码: {}, 查询语句: {}", device.getDeviceCode(), fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

//            if (results.size() >= 2) {
//                // 用最后一个点减去第一个点
//                Double end = results.get(results.size() - 1).getTotal();
//                Double start = results.get(0).getTotal();
//                if (end != null && start != null) {
//                    return Math.max(0, end - start);
//                }
//            }
//
//            return 0.0;
            return calculateElectricityWithNullHandling(results);
        } catch (Exception e) {
            log.error("计算上月用电量失败 - 设备编码: {}", device.getDeviceCode(), e);
            return 0.0;
        }
    }

    /**
     * 计算自定义时间范围用电量
     */
    public Double calculateCustomElectricity(Device device, String startTime, String endTime, String roomIds) {
        try {
            Instant startInstant = parseDateToInstant(startTime, true);
            Instant endInstant = parseDateToInstant(endTime, false);

            // 为了计算差值，我们需要获取开始时间前一天的数据点
//            Instant queryStart = startInstant.minusSeconds(86400);

            // 查询自定义时间范围的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder().bucket(influxDBProperties.getBucket()).measurement("device").fields("Total").tag("deviceCode", device.getDeviceCode()).tag("categoryId", device.getCategoryId().toString()).pivot().window("1d", "last").sort("_time", InfluxQueryBuilder.SORT_DESC).range(startInstant, endInstant);

            if (StringUtils.isNotBlank(roomIds)) {
                List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                if (!roomIdList.isEmpty()) {
                    String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                    builder.filter("(" + roomFilter + ")");
                }
            } else {
                builder.tag("roomId", device.getDeviceRoom().toString());
            }

            String fluxQuery = builder.build();
            log.info("查询自定义时间用电量 - 设备编码: {}, 开始时间: {}, 结束时间: {}, 查询语句: {}", device.getDeviceCode(), startInstant, endInstant, fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);
            //排序
            results.sort(Comparator.comparing(InfluxMqttPlug::getTime));
//            if (!results.isEmpty()) {
//                // 查找最接近结束时间的有效数据点
//                InfluxMqttPlug endData = null;
//                for (int i = results.size() - 1; i >= 0; i--) {
//                    if (results.get(i).getTotal() != null) {
//                        endData = results.get(i);
//                        break;
//                    }
//                }
//
//                // 查找最接近开始时间的有效数据点
//                InfluxMqttPlug startData = null;
//                for (InfluxMqttPlug result : results) {
//                    if (result.getTotal() != null) {
//                        startData = result;
//                        break;
//                    }
//                }
//
//                if (startData != null && endData != null &&
//                        startData.getTotal() != null && endData.getTotal() != null) {
//                    return Math.max(0, endData.getTotal() - startData.getTotal());
//                }
//            }
//
//            return 0.0;
            return calculateElectricityWithNullHandling(results);
        } catch (Exception e) {
            log.error("计算自定义时间用电量失败 - 设备编码: {}, 开始时间: {}, 结束时间: {}", device.getDeviceCode(), startTime, endTime, e);
            return 0.0;
        }
    }


    /**
     * 将日期字符串解析为Instant对象
     * @param dateStr 日期字符串，格式为 yyyy-MM-dd
     * @param isStart 是否为开始时间（true: 00:00:00, false: 23:59:59）
     * @return Instant对象
     */
    private Instant parseDateToInstant(String dateStr, boolean isStart) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (isStart) {
                return date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            } else {
                return date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
            }
        } catch (Exception e) {
            log.error("日期解析失败: {}", dateStr, e);
            // 返回默认值
            if (isStart) {
                return LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            } else {
                return LocalDate.now().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
            }
        }
    }

    @NotNull
    public PageResult<RoomsElectricityVO> getRoomsElectricityVOPageResult(Integer pageNum, Integer pageSize, String roomIds, String startTime, String endTime, String range, String categoryName, boolean isExport) {
        try {
            if (StringUtils.isEmpty(roomIds)) {
                List<Room> roomList = roomService.list(new LambdaQueryWrapper<Room>().eq(Room::getIsDeleted, 0));
                if (ObjectUtils.isEmpty(roomList)) {
                    return PageResult.success(new Page<>());
                }
                roomIds = roomList.stream().map(Room::getId).map(String::valueOf).collect(Collectors.joining(","));
            }
            Long categoryId = null;
            if (StringUtils.isNotEmpty(categoryName)) {
                //根据名称查询categoryId
                Category category = categoryService.getCategoryByName(categoryName);
                if (ObjectUtils.isNotEmpty(category)) {
                    categoryId = category.getId();
                }
            }
            List<RoomsElectricityVO> roomsElectricityList = getRoomsElectricity(startTime, endTime, roomIds, range, categoryId);

            // 按用电量升序排列（从低到高）
            roomsElectricityList.sort(Comparator.comparingDouble(RoomsElectricityVO::getTotalElectricity));

            // 如果不是导出场景，过滤掉totalElectricity为0.0的记录
            if (!isExport) {
                roomsElectricityList = roomsElectricityList.stream().filter(vo -> vo.getTotalElectricity() != null && vo.getTotalElectricity() != 0.0).collect(Collectors.toList());
            }

            // 分页处理
            int total = roomsElectricityList.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            List<RoomsElectricityVO> pagedResult = new ArrayList<>();
            if (fromIndex < total) {
                pagedResult = roomsElectricityList.subList(fromIndex, toIndex);
            }

            // 构造分页结果
            Page<RoomsElectricityVO> page = new Page<>(pageNum, pageSize);
            page.setTotal(total);
            page.setRecords(pagedResult);

            return PageResult.success(page);
        } catch (Exception e) {
            log.error("分页查询房间用电量失败 - roomIds: {}", roomIds, e);
            return PageResult.success(null);
        }
    }

    /**
     * 根据时间范围获取房间的用电量
     */
    public List<RoomsElectricityVO> getRoomsElectricity(String startTime, String endTime, String roomIds, String range, Long categoryId) {
        // 查询所有房间
        List<Room> roomList = roomService.list(new QueryWrapper<Room>().in("id", Arrays.stream(roomIds.split(",")).map(Long::parseLong).toList()));

        List<RoomsElectricityVO> resultList = new ArrayList<>();

        // 根据不同的时间范围计算用电量
        for (Room room : roomList) {
            // 使用已有的计算方法根据range计算用电量
            Double totalElectricity = calculateRoomElectricity(room.getId(), range, startTime, endTime, categoryId);
            RoomsElectricityVO vo = new RoomsElectricityVO();
            vo.setRoomId(room.getId());
            vo.setRoomName(room.getClassroomCode());
            vo.setTotalElectricity(MathUtils.formatDouble(totalElectricity));
            resultList.add(vo);
        }

        return resultList;
    }

    // 辅助方法
    public double calculateDifference(Double next, Double current) {
        if (next == null || current == null) {
            return 0.0; // 任何一个值为null，用电量设为0
        }

        Double difference = MathUtils.formatDouble(next - current);
        return difference != null ? Math.max(0, difference) : 0.0;
    }

    /**
     * 通用用电量计算方法 - 处理可能包含null值的数据点
     * @param results 查询结果列表
     * @return 计算得到的用电量
     */
    private Double calculateElectricityWithNullHandling(List<InfluxMqttPlug> results) {
        if (results.size() < 2) {
            return 0.0;
        }

        // 查找最后一个有效的数据点
        Double end = null;
        for (int i = results.size() - 1; i >= 0; i--) {
            if (results.get(i).getTotal() != null) {
                end = results.get(i).getTotal();
                break;
            }
        }

        // 查找第一个有效的数据点
        Double start = null;
        for (InfluxMqttPlug result : results) {
            if (result.getTotal() != null) {
                start = result.getTotal();
                break;
            }
        }

        // 确保找到了有效的开始和结束数据点
        if (end != null && start != null) {
            return MathUtils.formatDouble(Math.max(0, end - start));
        }

        return 0.0;
    }

    /**
     * 查询最近7天的每日用电量数据，适配CategoryElectricityVO格式
     * @param device 设备对象
     * @param roomIds 房间ID列表
     * @return 包含时间和用电量值的列表，适配CategoryElectricityVO格式
     */
    public CategoryElectricityData getWeeklyElectricityDataForCategory(Device device, String roomIds) {
        CategoryElectricityData categoryData = new CategoryElectricityData();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        try {
            // 获取当前日期
            LocalDate today = LocalDate.now();

            // 获取本周周一和上周周一
            LocalDate thisMonday = today.with(java.time.DayOfWeek.MONDAY);
            LocalDate lastMonday = thisMonday.minusWeeks(1);

            // 计算每天的用电量（与昨日用电量方法保持一致的计算方式）
            for (int i = 6; i >= 0; i--) {
                LocalDate targetDate = today.minusDays(i);

                // 构造当天的开始和结束时间
                Instant startOfDay = targetDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
                Instant endOfDay = targetDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

                // 查询当天的数据
                InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder().bucket(influxDBProperties.getBucket()).measurement("device").fields("Total").tag("deviceCode", device.getDeviceCode()).tag("categoryId", device.getCategoryId().toString()).pivot()
//                        .window("1h", "last")
                        .sort("_time", InfluxQueryBuilder.SORT_ASC).range(startOfDay, endOfDay);

                if (StringUtils.isNotBlank(roomIds)) {
                    List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                    if (!roomIdList.isEmpty()) {
                        String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                        builder.filter("(" + roomFilter + ")");
                    }
                } else {
                    builder.tag("roomId", device.getDeviceRoom().toString());
                }

                String fluxQuery = builder.build();
                log.info("查询一周{}用电量数据 - 设备编码: {}, 查询语句: {}", targetDate, device.getDeviceCode(), fluxQuery);

                List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

                // 使用与calculateYesterdayElectricity相同的方法计算当天用电量
                Double dailyElectricity = calculateElectricityWithNullHandling(results);

                // 添加时间标签和用电量值
                times.add(getDayOfWeekWithPrefix(targetDate, lastMonday, thisMonday));
                values.add(dailyElectricity != null ? dailyElectricity : 0.0);
            }

        } catch (Exception e) {
            log.error("查询近7天用电量数据失败 - 设备编码: {}", device.getDeviceCode(), e);
        }

        categoryData.setTime(times);
        categoryData.setValue(values);
        return categoryData;
    }

    /**
     * 查询多个设备最近7天的每日用电量数据，适配CategoryElectricityVO格式
     * @param deviceList 设备对象列表
     * @param roomIds 房间ID列表
     * @return 包含时间和用电量值的列表，适配CategoryElectricityVO格式
     */
    public CategoryElectricityData getWeeklyElectricityDataForCategory(List<Device> deviceList, String roomIds) {
        CategoryElectricityData categoryData = new CategoryElectricityData();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        try {
            // 获取当前日期
            LocalDate today = LocalDate.now();

            // 获取本周周一和上周周一
            LocalDate thisMonday = today.with(java.time.DayOfWeek.MONDAY);
            LocalDate lastMonday = thisMonday.minusWeeks(1);

            // 用于累计每天的用电量
            Map<String, Double> dailyValues = new LinkedHashMap<>();

            // 遍历所有设备
            for (Device device : deviceList) {
                try {
                    // 计算每天的用电量
                    for (int i = 6; i >= 0; i--) {
                        LocalDate targetDate = today.minusDays(i);

                        // 构造当天的开始和结束时间
                        Instant startOfDay = targetDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
                        Instant endOfDay = targetDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

                        // 查询当天的数据
                        InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                                .bucket(influxDBProperties.getBucket())
                                .measurement("device").fields("Total")
                                .tag("deviceCode", device.getDeviceCode())
                                .tag("categoryId", device.getCategoryId().toString())
                                .pivot()
                                .window("1h", "last")
                                .sort("_time", InfluxQueryBuilder.SORT_ASC)
                                .range(startOfDay, endOfDay);

                        if (StringUtils.isNotBlank(roomIds)) {
                            List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                            if (!roomIdList.isEmpty()) {
                                String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                                builder.filter("(" + roomFilter + ")");
                            }
                        } else {
                            builder.tag("roomId", device.getDeviceRoom().toString());
                        }

                        String fluxQuery = builder.build();
                        log.info("查询分类这一周的{}用电量数据 - 设备编码: {}, 查询语句: {}", targetDate, device.getDeviceCode(), fluxQuery);

                        List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

                        // 使用与calculateYesterdayElectricity相同的方法计算当天用电量
                        Double dailyElectricity = calculateElectricityWithNullHandling(results);

                        // 获取日期标签
                        String dayLabel = getDayOfWeekWithPrefix(targetDate, lastMonday, thisMonday);

                        // 累加用电量
                        if (dailyElectricity != null) {
                            dailyValues.merge(dayLabel, dailyElectricity, Double::sum);
                        }
                    }
                } catch (Exception e) {
                    log.error("查询设备近7天用电量数据失败 - 设备编码: {}", device.getDeviceCode(), e);
                    // 继续处理其他设备
                }
            }

            // 将累计结果转换为列表
            times.addAll(dailyValues.keySet());
            values.addAll(dailyValues.values().stream().map(MathUtils::formatDouble).toList());

        } catch (Exception e) {
            log.error("查询多个设备近7天用电量数据失败", e);
        }

        categoryData.setTime(times);
        categoryData.setValue(values);
        return categoryData;
    }

    /**
     * 将LocalDate转换为带前缀的中文星期几（区分上周和本周）
     * @param date 日期
     * @param lastMonday 上周周一
     * @param thisMonday 本周周一
     * @return 带前缀的中文星期几
     */
    private String getDayOfWeekWithPrefix(LocalDate date, LocalDate lastMonday, LocalDate thisMonday) {
        String dayOfWeek = switch (date.getDayOfWeek()) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
            default -> "";
        };

        // 判断是上周还是本周
        if (!date.isBefore(lastMonday) && date.isBefore(thisMonday)) {
            return "上" + dayOfWeek;
        } else if (!date.isBefore(thisMonday)) {
            return "本" + dayOfWeek;
        }

        return dayOfWeek;
    }

    /**
     * 查询多个设备当日每小时用电量数据，适配CategoryElectricityVO格式
     * @param deviceList 设备对象列表
     * @param roomIds 房间ID列表
     * @return 包含时间和用电量值的列表，适配CategoryElectricityVO格式
     */
    public CategoryElectricityData getDailyElectricityDataForCategory(List<Device> deviceList, String roomIds) {
        CategoryElectricityData categoryData = new CategoryElectricityData();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        try {
            Instant now = Instant.now();
            Instant twentyFourHoursAgo = now.minusSeconds(24 * 60 * 60); // 24小时前

            // 初始化累计值数组
            Map<String, Double> hourlyValues = new LinkedHashMap<>();

            // 遍历所有设备
            for (Device device : deviceList) {
                try {
                    // 查询当日每小时的数据
                    InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                            .bucket(influxDBProperties.getBucket())
                            .measurement("device")
                            .fields("Total")
                            .tag("deviceCode", device.getDeviceCode())
                            .tag("categoryId", device.getCategoryId().toString())
                            .pivot()
                            .window("1h", "last")
                            .sort("_time", InfluxQueryBuilder.SORT_ASC)
                            .range(twentyFourHoursAgo, now);

                    if (StringUtils.isNotBlank(roomIds)) {
                        List<String> roomIdList = Arrays.stream(roomIds.split(","))
                                .map(String::trim)
                                .filter(StringUtils::isNotBlank)
                                .toList();

                        if (!roomIdList.isEmpty()) {
                            String roomFilter = roomIdList.stream()
                                    .map(id -> String.format("r.roomId == \"%s\"", id))
                                    .collect(Collectors.joining(" or "));
                            builder.filter("(" + roomFilter + ")");
                        }
                    } else {
                        builder.tag("roomId", device.getDeviceRoom().toString());
                    }

                    String fluxQuery = builder.build();
                    log.info("查询当日每小时用电量数据 - 设备编码: {}, 查询语句: {}", device.getDeviceCode(), fluxQuery);

                    List<InfluxMqttPlug> results = influxDBClient.getQueryApi()
                            .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

                    // 处理每个设备的小时数据
                    Map<String, Double> deviceHourlyValues = new LinkedHashMap<>();
                    for (int i = 1; i < results.size(); i++) {
                        InfluxMqttPlug current = results.get(i);
                        InfluxMqttPlug previous = results.get(i - 1);

                        // 获取当前和前一个数据点的时间
                        Instant currentInstant = current.getTime();

                        // 格式化时间为小时（使用当前数据点的时间）
                        String hour = currentInstant.atZone(ZoneId.systemDefault()).getHour() + ":00";
                        log.info("数据的时间是{}", hour);

                        // 检查是否已经处理过这个小时的数据点（仅针对当前设备）
                        if (deviceHourlyValues.containsKey(hour)) {
                            log.info("设备{}跳过重复的时间点: {}", device.getDeviceCode(), hour);
                            continue;
                        }

                        // 如果current.getTotal为空，说明该时间用电量数据是没有的，按0算
                        if (current.getTotal() == null || previous.getTotal() == null) {
                            log.info("设备{}在{}时段用电量数据为空，按0计算", device.getDeviceCode(), hour);
                            // 累加0到对应小时的总值中
                            deviceHourlyValues.put(hour, 0.0);
                        } else {
                            // 计算该小时的用电量
                            double hourlyConsumption = Math.max(0, current.getTotal() - previous.getTotal());
                            log.info("设备{}在{}时段用电量: {} - {} = {}",
                                    device.getDeviceCode(), hour, current.getTotal(), previous.getTotal(), hourlyConsumption);
                            // 累加到当前设备的时间点中
                            deviceHourlyValues.put(hour, hourlyConsumption);
                        }
                    }

                    // 将当前设备的结果合并到总结果中
                    for (Map.Entry<String, Double> entry : deviceHourlyValues.entrySet()) {
                        hourlyValues.merge(entry.getKey(), entry.getValue(), Double::sum);
                    }
                } catch (Exception e) {
                    log.error("查询设备当日每小时用电量数据失败 - 设备编码: {}", device.getDeviceCode(), e);
                    // 继续处理其他设备
                }
            }

            // 将累计结果转换为列表
            times.addAll(hourlyValues.keySet());
            values.addAll(hourlyValues.values().stream()
                    .map(MathUtils::formatDouble)
                    .toList());

        } catch (Exception e) {
            log.error("查询多个设备当日每小时用电量数据失败", e);
        }

        categoryData.setTime(times);
        categoryData.setValue(values);
        return categoryData;
    }



    /**
     * 查询多个设备最近30天的每日用电量数据，适配CategoryElectricityVO格式
     * @param deviceList 设备对象列表
     * @param roomIds 房间ID列表
     * @return 包含时间和用电量值的列表，适配CategoryElectricityVO格式
     */
    public CategoryElectricityData getMonthlyElectricityDataForCategory(List<Device> deviceList, String roomIds) {
        CategoryElectricityData categoryData = new CategoryElectricityData();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        try {
            LocalDate today = LocalDate.now();
            LocalDate thirtyDaysAgo = today.minusDays(29); // 30天前（包含今天）

            // 获取本月1号和上个月1号
            LocalDate firstDayOfThisMonth = today.withDayOfMonth(1);
            LocalDate firstDayOfLastMonth = firstDayOfThisMonth.minusMonths(1);

            // 用于累计每天的用电量
            Map<String, Double> dailyValues = new LinkedHashMap<>();

            // 遍历所有设备
            for (Device device : deviceList) {
                try {
                    // 计算每天的用电量
                    for (int i = 0; i < 30; i++) {
                        LocalDate targetDate = thirtyDaysAgo.plusDays(i);

                        // 构造当天的开始和结束时间
                        Instant startOfDay = targetDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
                        Instant endOfDay = targetDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

                        // 查询当天的数据
                        InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                                .bucket(influxDBProperties.getBucket())
                                .measurement("device")
                                .fields("Total")
                                .tag("deviceCode", device.getDeviceCode())
                                .tag("categoryId", device.getCategoryId().toString())
                                .pivot()
                                .window("1h", "last").sort("_time", InfluxQueryBuilder.SORT_ASC)
                                .range(startOfDay, endOfDay);

                        if (StringUtils.isNotBlank(roomIds)) {
                            List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                            if (!roomIdList.isEmpty()) {
                                String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                                builder.filter("(" + roomFilter + ")");
                            }
                        } else {
                            builder.tag("roomId", device.getDeviceRoom().toString());
                        }

                        String fluxQuery = builder.build();
                        log.info("查询{}用电量数据 - 设备编码: {}, 查询语句: {}", targetDate, device.getDeviceCode(), fluxQuery);

                        List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

                        // 使用与calculateYesterdayElectricity相同的方法计算当天用电量
                        Double dailyElectricity = calculateElectricityWithNullHandling(results);
                        log.info("设备{}在{}的用电量: {}", device.getDeviceCode(), targetDate, dailyElectricity);
                        // 获取日期标签
                        String dayLabel;
                        if (!targetDate.isBefore(firstDayOfLastMonth) && targetDate.isBefore(firstDayOfThisMonth)) {
                            dayLabel = targetDate.getMonth().getValue() + "-" + targetDate.getDayOfMonth();
                        } else if (!targetDate.isBefore(firstDayOfThisMonth)) {
                            dayLabel = targetDate.getMonth().getValue() + "-" + targetDate.getDayOfMonth();
                        } else {
                            dayLabel = targetDate.getMonth().getValue() + "-" + targetDate.getDayOfMonth();
                        }

                        // 累加用电量
                        if (dailyElectricity != null) {
                            dailyValues.merge(dayLabel, dailyElectricity, Double::sum);
                        }
                    }
                } catch (Exception e) {
                    log.error("查询设备近30天用电量数据失败 - 设备编码: {}", device.getDeviceCode(), e);
                    // 继续处理其他设备
                }
            }

            // 将累计结果转换为列表
            times.addAll(dailyValues.keySet());
            values.addAll(dailyValues.values().stream().map(MathUtils::formatDouble).toList());

        } catch (Exception e) {
            log.error("查询多个设备近30天用电量数据失败", e);
        }

        categoryData.setTime(times);
        categoryData.setValue(values);
        return categoryData;
    }

    /**
     * 查询多个设备最近12个月的每月用电量数据，适配CategoryElectricityVO格式
     * @param deviceList 设备对象列表
     * @param roomIds 房间ID列表
     * @return 包含时间和用电量值的列表，适配CategoryElectricityVO格式
     */
    public CategoryElectricityData getYearlyElectricityDataForCategory(List<Device> deviceList, String roomIds) {
        CategoryElectricityData categoryData = new CategoryElectricityData();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        try {
            LocalDate today = LocalDate.now();
            LocalDate twelveMonthsAgo = today.minusMonths(11); // 12个月前（包含本月）

            // 获取当前年份
            int currentYear = today.getYear();

            // 用于累计每月的用电量
            Map<String, Double> monthlyValues = new LinkedHashMap<>();

            // 遍历所有设备
            for (Device device : deviceList) {
                try {
                    // 计算每个月的用电量
                    for (int i = 0; i < 12; i++) {
                        LocalDate targetMonth = twelveMonthsAgo.plusMonths(i);

                        // 构造当月的开始和结束时间
                        LocalDate firstDayOfMonth = targetMonth.withDayOfMonth(1);
                        LocalDate firstDayOfNextMonth = targetMonth.plusMonths(1).withDayOfMonth(1);

                        Instant startOfMonth = firstDayOfMonth.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
                        Instant endOfMonth = firstDayOfNextMonth.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().minusSeconds(1); // 减去1秒得到月末时间

                        // 查询当月的数据
                        InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder().bucket(influxDBProperties.getBucket()).measurement("device").fields("Total").tag("deviceCode", device.getDeviceCode())
//                                .tag("categoryId", device.getCategoryId().toString())
                                .pivot().window("1mo", "last").sort("_time", InfluxQueryBuilder.SORT_ASC).range(startOfMonth, endOfMonth);

                        if (StringUtils.isNotBlank(roomIds)) {
                            List<String> roomIdList = Arrays.stream(roomIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();

                            if (!roomIdList.isEmpty()) {
                                String roomFilter = roomIdList.stream().map(id -> String.format("r.roomId == \"%s\"", id)).collect(Collectors.joining(" or "));
                                builder.filter("(" + roomFilter + ")");
                            }
                        } else {
                            builder.tag("roomId", device.getDeviceRoom().toString());
                        }

                        String fluxQuery = builder.build();
                        log.info("查询{}年{}月用电量数据 - 设备编码: {}, 查询语句: {}", targetMonth.getYear(), targetMonth.getMonthValue(), device.getDeviceCode(), fluxQuery);

                        List<InfluxMqttPlug> results = influxDBClient.getQueryApi().query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);
                        if (ObjectUtils.isNotEmpty(results)) {
                            log.info("查询设备{}{}年{}月的数据为{}", device.getDeviceCode(), targetMonth.getYear(), targetMonth.getMonthValue(), results);
                        }
                        // 使用与calculateLastMonthElectricity相同的方法计算当月用电量
                        Double monthlyElectricity = calculateElectricityWithNullHandling(results);

                        // 获取月份标签
                        String monthLabel;
                        if (targetMonth.getYear() < currentYear) {
                            monthLabel = targetMonth.getYear() + "-" + targetMonth.getMonthValue();
                        } else if (targetMonth.getYear() == currentYear) {
                            monthLabel = targetMonth.getYear() + "-" + targetMonth.getMonthValue();
                        } else {
                            monthLabel = targetMonth.getYear() + "-" + targetMonth.getMonthValue();
                        }

                        // 累加用电量
                        if (monthlyElectricity != null) {
                            monthlyValues.merge(monthLabel, monthlyElectricity, Double::sum);
                        }
                    }
                } catch (Exception e) {
                    log.error("查询设备近12个月用电量数据失败 - 设备编码: {}", device.getDeviceCode(), e);
                    // 继续处理其他设备
                }
            }

            // 将累计结果转换为列表
            times.addAll(monthlyValues.keySet());
            values.addAll(monthlyValues.values().stream().map(MathUtils::formatDouble).toList());

        } catch (Exception e) {
            log.error("查询多个设备近12个月用电量数据失败", e);
        }

        categoryData.setTime(times);
        categoryData.setValue(values);
        return categoryData;
    }
}
