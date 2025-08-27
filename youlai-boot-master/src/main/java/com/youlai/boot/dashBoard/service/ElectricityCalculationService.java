package com.youlai.boot.dashBoard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;
import com.youlai.boot.categoryDeviceRelationship.service.CategoryDeviceRelationshipService;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.util.InfluxQueryBuilder;
import com.youlai.boot.common.util.MathUtils;
import com.youlai.boot.config.property.InfluxDBProperties;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
    private final CategoryDeviceRelationshipService categoryDeviceRelationshipService;

    /**
     * 计算设备用电量
     */
    public Double calculateDeviceElectricity(String deviceCode, String range, String startTime, String endTime, String roomIds) {
        try {
            Double totalElectricity = switch (range) {
                case "today" -> calculateTodayElectricity(deviceCode, roomIds);
                case "yesterday" -> calculateYesterdayElectricity(deviceCode, roomIds);
                case "week" -> calculateWeekElectricity(deviceCode, roomIds);
                case "lastWeek" -> calculateLastWeekElectricity(deviceCode, roomIds);
                case "month" -> calculateMonthElectricity(deviceCode, roomIds);
                case "lastMonth" -> calculateLastMonthElectricity(deviceCode, roomIds);
                default -> calculateCustomElectricity(deviceCode, startTime, endTime, roomIds);
            };

            log.info("设备用电量计算 - 设备编码: {}, 范围: {}, 用电量: {}", deviceCode, range, totalElectricity);
            return MathUtils.formatDouble(totalElectricity);
        } catch (Exception e) {
            log.error("计算设备用电量失败 - 设备编码: {}, 范围: {}, 异常信息: ", deviceCode, range, e);
            return 0.0;
        }
    }

    /**
     * 计算房间用电量
     */
    public Double calculateRoomElectricity(Long roomId, String range, String startTime, String endTime, Long categoryId) {
        try {
            // 获取房间内所有主设备
            List<Device> roomDevices = deviceService.list(new QueryWrapper<Device>()
                    .eq("device_room", roomId)
                    .eq("is_master", 1));
            List<CategoryDeviceRelationship> categoryDeviceRelationships = new ArrayList<>();
            if (ObjectUtils.isNotEmpty(categoryId)) {
                categoryDeviceRelationships = categoryDeviceRelationshipService.listByCategoryId(categoryId);
            }
            double roomTotalElectricity = 0.0;
            for (Device device : roomDevices) {
                if (ObjectUtils.isNotEmpty(categoryDeviceRelationships)) {
                    // 检查当前设备是否在分类设备关系列表中
                    boolean deviceInCategory = categoryDeviceRelationships.stream()
                            .anyMatch(relation -> relation.getDeviceId().equals(device.getId()));

                    // 如果设备不属于指定分类，则跳过
                    if (!deviceInCategory) {
                        continue;
                    }
                    Double deviceElectricity = calculateDeviceElectricity(
                            device.getDeviceCode(), range, startTime, endTime, roomId.toString());
                    if (deviceElectricity != null) {
                        roomTotalElectricity += deviceElectricity;
                    }
                } else {
                    Double deviceElectricity = calculateDeviceElectricity(
                            device.getDeviceCode(), range, startTime, endTime, roomId.toString());
                    if (deviceElectricity != null) {
                        roomTotalElectricity += deviceElectricity;
                    }
                }
            }

            return MathUtils.formatDouble(roomTotalElectricity);
        } catch (Exception e) {
            log.error("计算房间用电量失败 - roomId: {}, range: {}", roomId, range, e);
            return 0.0;
        }
    }

//    /**
//     * 计算部门用电量
//     */
//    public Double calculateDepartmentElectricity(Long departmentId, String range, String startTime, String endTime) {
//        try {
//            // 查询该部门下所有房间
//            List<Room> rooms = roomService.list(new QueryWrapper<Room>()
//                    .eq("department_id", departmentId));
//
//            double departmentTotalElectricity = 0.0;
//            for (Room room : rooms) {
//                Double roomElectricity = calculateRoomElectricity(
//                        room.getId(), range, startTime, endTime);
//                if (roomElectricity != null) {
//                    departmentTotalElectricity += roomElectricity;
//                }
//            }
//
//            return departmentTotalElectricity;
//        } catch (Exception e) {
//            log.error("计算部门用电量失败 - departmentId: {}, range: {}", departmentId, range, e);
//            return 0.0;
//        }
//    }

    /**
     * 计算今日用电量
     */
    private Double calculateTodayElectricity(String deviceCode, String roomIds) {
        try {
            Instant now = Instant.now();
            Instant startOfDay = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();

            // 查询今日0点至今的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .tag("deviceCode", deviceCode)
                    .pivot()
                    .window("1d", "last")
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .range(startOfDay, now);

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
            }

            String fluxQuery = builder.build();
            log.info("查询今日用电量 - 设备编码: {}, 查询语句: {}", deviceCode, fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (results.size() >= 2) {
                // 用最后一个点减去倒数第二个点
                Double current = results.get(results.size() - 1).getTotal();
                Double start = results.get(results.size() - 2).getTotal();
                if (current != null && start != null) {
                    return MathUtils.formatDouble(Math.max(0, current - start));
                }
            }

            return 0.0;
        } catch (Exception e) {
            log.error("计算今日用电量失败 - 设备编码: {}", deviceCode, e);
            return 0.0;
        }
    }

    /**
     * 计算昨日用电量
     */
    private Double calculateYesterdayElectricity(String deviceCode, String roomIds) {
        try {
            Instant now = Instant.now();
            Instant startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Instant endOfYesterday = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();

            // 查询昨日0点到今日0点的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .tag("deviceCode", deviceCode)
                    .pivot()
                    .window("1d", "last")
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .range(startOfYesterday, now);

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
            }

            String fluxQuery = builder.build();
            log.info("查询昨日用电量 - 设备编码: {}, 查询语句: {}", deviceCode, fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (results.size() >= 3) {
                // 用倒数第二个点减去倒数第三个点
                Double end = results.get(results.size() - 2).getTotal();
                Double start = results.get(results.size() - 3).getTotal();
                if (end != null && start != null) {
                    return Math.max(0, end - start);
                }
            }

            return 0.0;
        } catch (Exception e) {
            log.error("计算昨日用电量失败 - 设备编码: {}", deviceCode, e);
            return 0.0;
        }
    }

    /**
     * 计算本周用电量
     */
    private Double calculateWeekElectricity(String deviceCode, String roomIds) {
        try {
            Instant now = Instant.now();
            LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            Instant startOfWeek = monday.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();

            // 查询本周一0点至今的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .tag("deviceCode", deviceCode)
                    .pivot()
                    .window("1d", "last")
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .range(startOfWeek, now);

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
            }

            String fluxQuery = builder.build();
            log.info("查询本周用电量 - 设备编码: {}, 查询语句: {}", deviceCode, fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (results.size() >= 2) {
                // 查找最后一个有效的数据点
                Double current = null;
                for (int i = results.size() - 1; i >= 0; i--) {
                    if (results.get(i).getTotal() != null) {
                        current = results.get(i).getTotal();
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

                if (current != null && start != null) {
                    return Math.max(0, current - start);
                }
            }

            return 0.0;
        } catch (Exception e) {
            log.error("计算本周用电量失败 - 设备编码: {}", deviceCode, e);
            return 0.0;
        }
    }


    /**
     * 计算上周用电量
     */
    private Double calculateLastWeekElectricity(String deviceCode, String roomIds) {
        try {
            LocalDate lastMonday = LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
            LocalDate thisMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            Instant startOfLastWeek = lastMonday.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Instant endOfLastWeek = thisMonday.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Instant now = Instant.now();

            // 查询上周一0点到本周一0点的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .tag("deviceCode", deviceCode)
                    .pivot()
                    .window("1d", "last")
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .range(startOfLastWeek, now);

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
            }

            String fluxQuery = builder.build();
            log.info("查询上周用电量 - 设备编码: {}, 查询语句: {}", deviceCode, fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (results.size() >= 3) {
                // 查找倒数第二个有效的数据点（上周日的数据）
                Double end = null;
                for (int i = results.size() - 2; i >= 0; i--) {
                    if (results.get(i).getTotal() != null) {
                        end = results.get(i).getTotal();
                        break;
                    }
                }

                // 查找第一个有效的数据点（上周一的数据）
                Double start = null;
                for (InfluxMqttPlug result : results) {
                    if (result.getTotal() != null) {
                        start = result.getTotal();
                        break;
                    }
                }

                if (end != null && start != null) {
                    return Math.max(0, end - start);
                }
            }

            return 0.0;
        } catch (Exception e) {
            log.error("计算上周用电量失败 - 设备编码: {}", deviceCode, e);
            return 0.0;
        }
    }

    /**
     * 计算本月用电量
     */
    private Double calculateMonthElectricity(String deviceCode, String roomIds) {
        try {
            Instant now = Instant.now();
            LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
            Instant startOfMonth = firstDayOfMonth.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();

            // 查询本月1号0点至今的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .tag("deviceCode", deviceCode)
                    .pivot()
                    .window("1d", "last")
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .range(startOfMonth, now);

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
            }

            String fluxQuery = builder.build();
            log.info("查询本月用电量 - 设备编码: {}, 查询语句: {}", deviceCode, fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (results.size() >= 2) {
                // 查找最后一个有效的数据点
                Double current = null;
                for (int i = results.size() - 1; i >= 0; i--) {
                    if (results.get(i).getTotal() != null) {
                        current = results.get(i).getTotal();
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

                if (current != null && start != null) {
                    return Math.max(0, current - start);
                }
            }

            return 0.0;
        } catch (Exception e) {
            log.error("计算本月用电量失败 - 设备编码: {}", deviceCode, e);
            return 0.0;
        }
    }

    /**
     * 计算上月用电量
     */
    private Double calculateLastMonthElectricity(String deviceCode, String roomIds) {
        try {
            LocalDate firstDayOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            LocalDate firstDayOfThisMonth = LocalDate.now().withDayOfMonth(1);
            Instant startOfLastMonth = firstDayOfLastMonth.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Instant endOfLastMonth = firstDayOfThisMonth.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Instant now = Instant.now();

            // 查询上月1号0点到本月1号0点的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .tag("deviceCode", deviceCode)
                    .pivot()
                    .window("1d", "last")
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .range(startOfLastMonth, now);

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
            }

            String fluxQuery = builder.build();
            log.info("查询上月用电量 - 设备编码: {}, 查询语句: {}", deviceCode, fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (results.size() >= 3) {
                // 查找倒数第二个有效的数据点（本月1号的数据）
                Double end = null;
                for (int i = results.size() - 2; i >= 0; i--) {
                    if (results.get(i).getTotal() != null) {
                        end = results.get(i).getTotal();
                        break;
                    }
                }

                // 查找第一个有效的数据点（上月1号的数据）
                Double start = null;
                for (InfluxMqttPlug result : results) {
                    if (result.getTotal() != null) {
                        start = result.getTotal();
                        break;
                    }
                }

                if (end != null && start != null) {
                    return Math.max(0, end - start);
                }
            }

            return 0.0;
        } catch (Exception e) {
            log.error("计算上月用电量失败 - 设备编码: {}", deviceCode, e);
            return 0.0;
        }
    }

    /**
     * 计算自定义时间范围用电量
     */
    private Double calculateCustomElectricity(String deviceCode, String startTime, String endTime, String roomIds) {
        try {
            Instant startInstant = parseDateToInstant(startTime, true);
            Instant endInstant = parseDateToInstant(endTime, false);

            // 为了计算差值，我们需要获取开始时间前一天的数据点
            Instant queryStart = startInstant.minusSeconds(86400);

            // 查询自定义时间范围的数据
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .tag("deviceCode", deviceCode)
                    .pivot()
                    .window("1d", "last")
                    .sort("_time", InfluxQueryBuilder.SORT_DESC)
                    .range(queryStart, endInstant);

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
            }

            String fluxQuery = builder.build();
            log.info("查询自定义时间用电量 - 设备编码: {}, 开始时间: {}, 结束时间: {}, 查询语句: {}",
                    deviceCode, startInstant, endInstant, fluxQuery);

            List<InfluxMqttPlug> results = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (!results.isEmpty()) {
                // 查找最接近结束时间的有效数据点
                InfluxMqttPlug endData = null;
                for (int i = results.size() - 1; i >= 0; i--) {
                    if (results.get(i).getTotal() != null) {
                        endData = results.get(i);
                        break;
                    }
                }

                // 查找最接近开始时间的有效数据点
                InfluxMqttPlug startData = null;
                for (InfluxMqttPlug result : results) {
                    if (result.getTotal() != null) {
                        startData = result;
                        break;
                    }
                }

                if (startData != null && endData != null &&
                        startData.getTotal() != null && endData.getTotal() != null) {
                    return Math.max(0, endData.getTotal() - startData.getTotal());
                }
            }

            return 0.0;
        } catch (Exception e) {
            log.error("计算自定义时间用电量失败 - 设备编码: {}, 开始时间: {}, 结束时间: {}",
                    deviceCode, startTime, endTime, e);
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
                roomsElectricityList = roomsElectricityList.stream()
                        .filter(vo -> vo.getTotalElectricity() != null && vo.getTotalElectricity() != 0.0)
                        .collect(Collectors.toList());
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
     * 确定时间范围类型
     */
    private String determineRangeType(String startTime, String endTime) {
        if (StringUtils.isBlank(startTime) && StringUtils.isBlank(endTime)) {
            return "today"; // 默认今天
        }

        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            return "custom";
        }

        return "custom";
    }

    /**
     * 根据时间范围获取房间用电量
     */
    public List<RoomsElectricityVO> getRoomsElectricity(String startTime, String endTime, String roomIds, String range, Long categoryId) {
        // 查询所有房间
        List<Room> roomList = roomService.list(new QueryWrapper<Room>().in("id", Arrays.stream(roomIds.split(","))
                .map(Long::parseLong)
                .toList()));

        List<RoomsElectricityVO> resultList = new ArrayList<>();

        // 根据不同的时间范围计算用电量
        for (Room room : roomList) {
            // 使用已有的计算方法根据range计算用电量
            Double totalElectricity = calculateRoomElectricity(room.getId(), range, startTime, endTime, categoryId);
            if (totalElectricity != 0.0) {
                RoomsElectricityVO vo = new RoomsElectricityVO();
                vo.setRoomId(room.getId());
                vo.setRoomName(room.getClassroomCode());
                vo.setTotalElectricity(MathUtils.formatDouble(totalElectricity));
                resultList.add(vo);
            }
        }

        return resultList;
    }

    /**
     * 查询指定时间点的房间电表读数
     *
     * @param dateTime 时间字符串
     * @param roomIds  房间ID列表
     * @param isStart  是否为起始时间
     * @return 电表读数列表
     */
    public List<InfluxMqttPlug> queryRoomsElectricityAtTime(String dateTime, String roomIds, boolean isStart) {
        try {
            // 根据时间范围构建查询
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .pivot()
                    .sort("_time", InfluxQueryBuilder.SORT_DESC);

            // 处理多roomId查询
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
            }

            // 解析时间
            Instant timeInstant;
            if (StringUtils.isBlank(dateTime)) {
                // 如果时间为空，使用默认值
                timeInstant = isStart ?
                        LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant() :
                        Instant.now(); // 结束时间使用当前时间
            } else {
                timeInstant = parseDateToInstant(dateTime, isStart);
            }

            // 设置时间范围查询
            builder.range(timeInstant.minusSeconds(86400), timeInstant);

            // 构建并执行查询语句
            String fluxQuery = builder.build();
            log.info("查询房间电表读数 - 时间点: {}, 查询语句: {}", timeInstant, fluxQuery);

            return influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);
        } catch (Exception e) {
            log.error("查询房间电表读数失败 - 时间: {}, 异常信息: ", dateTime, e);
            return new ArrayList<>();
        }
    }
}
