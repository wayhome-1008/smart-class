package com.youlai.boot.dashBoard.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;
import com.youlai.boot.categoryDeviceRelationship.service.CategoryDeviceRelationshipService;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.util.InfluxQueryBuilder;
import com.youlai.boot.common.util.MathUtils;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.dashBoard.model.vo.CategoryElectricityInfoVO;
import com.youlai.boot.dashBoard.model.vo.DepartmentCategoryElectricityInfoVO;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.system.model.entity.Config;
import com.youlai.boot.system.model.entity.Dept;
import com.youlai.boot.system.service.ConfigService;
import com.youlai.boot.system.service.DeptService;
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
 *@CreateTime: 2025-08-11  15:30
 *@Description: 数据汇总接口
 */
@Tag(name = "数据汇总接口")
@RestController
@RequestMapping("/api/v1/deviceData")
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeviceDataController {
    private final CategoryService categoryService;
    private final ConfigService configService;
    private final CategoryDeviceRelationshipService categoryDeviceRelationshipService;
    private final DeviceService deviceService;
    private final InfluxDBProperties influxDBProperties;
    private final com.influxdb.client.InfluxDBClient influxDBClient;
    private final com.youlai.boot.room.service.RoomService roomService;
    private final DeptService deptService;

    @Operation(summary = "分页查询各部门各分类用电量")
    @GetMapping("/department/category/electricity/page")
    public PageResult<DepartmentCategoryElectricityInfoVO> getDepartmentCategoryElectricityPage(
            @Parameter(description = "页码，默认为1")
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页数量，默认为10")
            @RequestParam(defaultValue = "10") Integer pageSize,

            @Parameter(description = "时间范围: today-今天/yesterday-昨天/week-本周/lastWeek-上周/month-本月/lastMonth-上月/custom-自定义")
            @RequestParam(defaultValue = "today") String range,

            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime,

            @Parameter(description = "部门IDs，多个用逗号分隔")
            @RequestParam(required = false) String deptIds,

            @Parameter(description = "房间IDs，多个用逗号分隔")
            @RequestParam(required = false) String roomIds) {

        try {
            // 1. 获取所有分类
            List<Category> categories = categoryService.list();
            if (ObjectUtils.isEmpty(categories)) {
                Page<DepartmentCategoryElectricityInfoVO> page = new Page<>(pageNum, pageSize);
                page.setTotal(0);
                page.setRecords(new ArrayList<>());
                return PageResult.success(page);
            }

            // 2. 筛选出有效的分类（在配置中存在的分类）
            List<String> categoryNames = categories.stream()
                    .map(Category::getCategoryName)
                    .collect(Collectors.toList());

            List<Config> configList = configService.listByKeys(categoryNames);
            List<Category> validCategories = new ArrayList<>();

            for (Config config : configList) {
                categories.stream()
                        .filter(category -> category.getCategoryName().equals(config.getConfigKey()))
                        .findFirst()
                        .ifPresent(validCategories::add);
            }

            // 3. 获取所有房间的信息，用于部门映射
            List<Room> allRooms = roomService.list();
            Map<Long, Room> roomMap = allRooms.stream()
                    .collect(Collectors.toMap(
                            Room::getId,
                            room -> room
                    ));

            // 4. 查询各部门各分类的用电量
            List<DepartmentCategoryElectricityInfoVO> departmentCategoryElectricityList = new ArrayList<>();

            // 按部门和分类分组统计
            Map<String, DepartmentCategoryElectricityInfoVO> departmentCategoryMap = new HashMap<>();

            for (Category category : validCategories) {
                List<CategoryDeviceRelationship> relationships =
                        categoryDeviceRelationshipService.listByCategoryId(category.getId());

                if (!ObjectUtils.isEmpty(relationships)) {
                    List<Long> deviceIds = relationships.stream()
                            .map(CategoryDeviceRelationship::getDeviceId)
                            .collect(Collectors.toList());

                    List<Device> masterDevices = deviceService.listByIds(deviceIds).stream()
                            .filter(device -> device.getIsMaster() == 1)
                            .toList();

                    for (Device masterDevice : masterDevices) {
                        Double deviceElectricity = getDeviceElectricityByRange(masterDevice.getDeviceCode(), range, startTime, endTime, roomIds);
                        if (deviceElectricity != null) {
                            // 获取设备所在房间
                            Room room = roomMap.get(masterDevice.getDeviceRoom());
                            if (room != null) {
                                Long departmentId = room.getDepartmentId();
                                Dept department = deptService.getById(departmentId);
                                String departmentName = department.getName(); // 假设Room实体中有departmentName字段

                                // 构造分组键：部门ID_分类ID
                                String groupKey = departmentId + "_" + category.getId();

                                DepartmentCategoryElectricityInfoVO vo = departmentCategoryMap.get(groupKey);
                                if (vo == null) {
                                    vo = new DepartmentCategoryElectricityInfoVO();
                                    vo.setDepartmentId(departmentId);
                                    vo.setDepartmentName(departmentName != null ? departmentName : "未知部门");
                                    vo.setCategoryId(category.getId());
                                    vo.setCategoryName(category.getCategoryName());
                                    vo.setTotalElectricity(0.0);
                                    vo.setDeviceCount(0);
                                    departmentCategoryMap.put(groupKey, vo);
                                }
                                // 累加用电量和设备数量
                                vo.setTotalElectricity(MathUtils.formatDouble(vo.getTotalElectricity() + deviceElectricity));
                                vo.setDeviceCount(vo.getDeviceCount() + 1);
                            }
                        }

                    }
                }
            }

            departmentCategoryElectricityList.addAll(departmentCategoryMap.values());

            // 添加部门筛选逻辑
            if (StringUtils.isNotBlank(deptIds)) {
                List<Long> deptIdList = Arrays.stream(deptIds.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .map(Long::parseLong)
                        .toList();

                departmentCategoryElectricityList = departmentCategoryElectricityList.stream()
                        .filter(item -> deptIdList.contains(item.getDepartmentId()))
                        .collect(Collectors.toList());
            }
            // 计算每个分类的总用电量和占比
// 按分类ID分组，计算每个分类的总用电量
            Map<Long, Double> categoryTotalElectricityMap = departmentCategoryElectricityList.stream()
                    .collect(Collectors.groupingBy(
                            DepartmentCategoryElectricityInfoVO::getCategoryId,
                            Collectors.summingDouble(DepartmentCategoryElectricityInfoVO::getTotalElectricity)
                    ));

// 为每个记录设置分类总用电量和占比
            for (DepartmentCategoryElectricityInfoVO item : departmentCategoryElectricityList) {
                Double categoryTotal = categoryTotalElectricityMap.get(item.getCategoryId());
                if (categoryTotal != null && categoryTotal > 0) {
                    item.setCategoryTotalElectricity(categoryTotal);
                } else {
                    item.setCategoryTotalElectricity(0.0);
                }
            }
            // 5. 分页处理
            int total = departmentCategoryElectricityList.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            List<DepartmentCategoryElectricityInfoVO> pagedResult = new ArrayList<>();
            if (fromIndex < total) {
                // 按部门和分类排序
                departmentCategoryElectricityList.sort(Comparator
                        .comparing(DepartmentCategoryElectricityInfoVO::getDepartmentName)
                        .thenComparing(DepartmentCategoryElectricityInfoVO::getCategoryName));

                pagedResult = departmentCategoryElectricityList.subList(fromIndex, toIndex);
            }

            // 6. 构造分页结果
            Page<DepartmentCategoryElectricityInfoVO> page = new Page<>(pageNum, pageSize);
            page.setTotal(total);
            page.setRecords(pagedResult);

            return PageResult.success(page);

        } catch (Exception e) {
            log.error("分页查询各部门各分类用电量失败: ", e);
        }
        return PageResult.success(null);
    }


    @Operation(summary = "分页查询各分类用电量")
    @GetMapping("/category/electricity/page")
    public PageResult<CategoryElectricityInfoVO> getCategoryElectricityPage(
            @Parameter(description = "页码，默认为1")
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页数量，默认为10")
            @RequestParam(defaultValue = "10") Integer pageSize,

            @Parameter(description = "时间范围: today-今天/yesterday-昨天/week-本周/lastWeek-上周/month-本月/lastMonth-上月/custom-自定义")
            @RequestParam(defaultValue = "today") String range,

            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime) {

        try {
            // 1. 获取所有分类
            List<Category> categories = categoryService.list();
            if (ObjectUtils.isEmpty(categories)) {
                Page<CategoryElectricityInfoVO> page = new Page<>(pageNum, pageSize);
                page.setTotal(0);
                page.setRecords(new ArrayList<>());
                return PageResult.success(page);
            }

            // 2. 筛选出有效的分类（在配置中存在的分类）
            List<String> categoryNames = categories.stream()
                    .map(Category::getCategoryName)
                    .collect(Collectors.toList());

            List<Config> configList = configService.listByKeys(categoryNames);
            List<Category> validCategories = new ArrayList<>();

            for (Config config : configList) {
                categories.stream()
                        .filter(category -> category.getCategoryName().equals(config.getConfigKey()))
                        .findFirst()
                        .ifPresent(validCategories::add);
            }

            // 3. 分页处理
            int total = validCategories.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            List<Category> pagedCategories = new ArrayList<>();
            if (fromIndex < total) {
                pagedCategories = validCategories.subList(fromIndex, toIndex);
            }

            // 4. 查询各分类的用电量
            List<CategoryElectricityInfoVO> categoryElectricityList = new ArrayList<>();
            for (Category category : pagedCategories) {
                CategoryElectricityInfoVO vo = getCategoryElectricityInfo(category, range, startTime, endTime);
                if (vo != null) {
                    categoryElectricityList.add(vo);
                }
            }

            // 5. 构造分页结果
            Page<CategoryElectricityInfoVO> page = new Page<>(pageNum, pageSize);
            page.setTotal(total);
            page.setRecords(categoryElectricityList);

            return PageResult.success(page);

        } catch (Exception e) {
            log.error("分页查询各分类用电量失败: ", e);
        }
        return PageResult.success(null);
    }

    /**
     * 获取单个分类的用电量信息
     */
    private CategoryElectricityInfoVO getCategoryElectricityInfo(Category category, String range, String startTime, String endTime) {
        try {
            CategoryElectricityInfoVO vo = new CategoryElectricityInfoVO();
            vo.setCategoryId(category.getId());
            vo.setCategoryName(category.getCategoryName());

            // 获取分类下的设备关系
            List<CategoryDeviceRelationship> relationships =
                    categoryDeviceRelationshipService.listByCategoryId(category.getId());

            if (ObjectUtils.isEmpty(relationships)) {
                vo.setTotalElectricity(0.0);
                vo.setDeviceCount(0);
                return vo;
            }

            // 获取设备ID列表
            List<Long> deviceIds = relationships.stream()
                    .map(CategoryDeviceRelationship::getDeviceId)
                    .collect(Collectors.toList());

            // 获取主设备列表
            List<Device> masterDevices = deviceService.listByIds(deviceIds).stream()
                    .filter(device -> device.getIsMaster() == 1)
                    .collect(Collectors.toList());

            vo.setDeviceCount(masterDevices.size());

            // 计算总用电量
            double totalElectricity = 0.0;
            for (Device masterDevice : masterDevices) {
                Double deviceElectricity = getDeviceElectricityByRange(masterDevice.getDeviceCode(), range, startTime, endTime, null);
                if (deviceElectricity != null) {
                    totalElectricity += deviceElectricity;
                }
            }

            vo.setTotalElectricity(MathUtils.formatDouble(totalElectricity));
            return vo;

        } catch (Exception e) {
            log.error("获取分类 {} 用电量信息失败: ", category.getCategoryName(), e);
            return null;
        }
    }


    /**
     * 根据时间范围获取设备用电量
     */
    private Double getDeviceElectricityByRange(String deviceCode, String range, String startTime, String endTime, String roomIds) {
        try {
            // 根据时间范围构建查询
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .tag("deviceCode", deviceCode)
                    .limit(1)
                    .pivot()
                    .sort("_time", InfluxQueryBuilder.SORT_DESC)
                    .timeShift("8h");
            // 处理多roomId查询
            if (StringUtils.isNotBlank(roomIds)) {
                List<String> roomIdList = Arrays.stream(roomIds.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .toList();

                if (!roomIdList.isEmpty()) {
                    // 构造 or 条件: r.roomId == "123" or r.roomId == "456" or r.roomId == "789"
                    String roomFilter = roomIdList.stream()
                            .map(id -> String.format("r.roomId == \"%s\"", id))
                            .collect(Collectors.joining(" or "));
                    builder.filter("(" + roomFilter + ")");
                }
            }
            // 根据不同时间范围设置查询参数
            switch (range) {
                case "today":
                    builder.today();
                    break;
                case "yesterday":
                    // 昨天需要特殊处理
                    builder.range(1, "d");
                    // 可以添加额外的过滤条件来精确匹配昨天的数据
                    break;
                case "week":
                    builder.currentWeek();
                    break;
                case "lastWeek":
                    builder.range(1, "w");
                    break;
                case "month":
                    builder.currentMonth();
                    break;
                case "lastMonth":
                    builder.range(1, "mo");
                    break;
                case "custom":
                    // 自定义时间范围使用默认的1天范围，通过更精确的过滤来处理
                    if (StringUtils.isNotBlank(startTime) || StringUtils.isNotBlank(endTime)) {
                        log.warn("当前InfluxQueryBuilder不支持自定义起止时间，使用默认1天范围");
                    }
                    builder.range(1, "d");
                    break;
                default:
                    builder.today();
                    break;
            }

            log.info("设备 {} 时间范围 {} 用电InfluxDB查询语句: {}", deviceCode, range, builder.build());
            List<InfluxMqttPlug> tables = influxDBClient.getQueryApi()
                    .query(builder.build(), influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (!tables.isEmpty()) {
                return tables.get(0).getTotal();
            }
            return null;
        } catch (Exception e) {
            log.error("查询设备 {} 时间范围 {} 用电量失败: ", deviceCode, range, e);
            return null;
        }
    }

    /**
     * 根据时间范围计算查询参数
     */
    private TimeRange calculateTimeRange(String range, String startTime, String endTime) {
        TimeRange timeRange = new TimeRange();
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        Instant now = Instant.now();

        switch (range) {
            case "today":
                // 今天
                timeRange.setStartTime(now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant());
                timeRange.setEndTime(now.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant());
                break;

            case "yesterday":
                // 昨天
                timeRange.setStartTime(now.atZone(zoneId).toLocalDate().minusDays(1).atStartOfDay(zoneId).toInstant());
                timeRange.setEndTime(now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant());
                break;

            case "week":
                // 本周
                timeRange.setStartTime(now.atZone(zoneId).with(java.time.DayOfWeek.MONDAY).toLocalDate().atStartOfDay(zoneId).toInstant());
                timeRange.setEndTime(now.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant());
                break;

            case "lastWeek":
                // 上周
                timeRange.setStartTime(now.atZone(zoneId).with(java.time.DayOfWeek.MONDAY).toLocalDate().minusWeeks(1).atStartOfDay(zoneId).toInstant());
                timeRange.setEndTime(now.atZone(zoneId).with(java.time.DayOfWeek.MONDAY).toLocalDate().atStartOfDay(zoneId).toInstant());
                break;

            case "month":
                // 本月
                timeRange.setStartTime(now.atZone(zoneId).toLocalDate().withDayOfMonth(1).atStartOfDay(zoneId).toInstant());
                timeRange.setEndTime(now.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant());
                break;

            case "lastMonth":
                // 上月
                timeRange.setStartTime(now.atZone(zoneId).toLocalDate().minusMonths(1).withDayOfMonth(1).atStartOfDay(zoneId).toInstant());
                timeRange.setEndTime(now.atZone(zoneId).toLocalDate().withDayOfMonth(1).atStartOfDay(zoneId).toInstant());
                break;

            case "custom":
                // 自定义时间范围
                if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        timeRange.setStartTime(java.time.LocalDate.parse(startTime, formatter).atStartOfDay(zoneId).toInstant());
                        timeRange.setEndTime(java.time.LocalDate.parse(endTime, formatter).plusDays(1).atStartOfDay(zoneId).toInstant());
                    } catch (Exception e) {
                        // 参数解析失败，使用默认今天
                        timeRange.setStartTime(now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant());
                        timeRange.setEndTime(now.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant());
                    }
                } else {
                    // 参数不完整，使用默认今天
                    timeRange.setStartTime(now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant());
                    timeRange.setEndTime(now.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant());
                }
                break;

            default:
                // 默认使用今天
                timeRange.setStartTime(now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant());
                timeRange.setEndTime(now.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant());
                break;
        }

        return timeRange;
    }

    /**
     * 时间范围参数类
     */
    private static class TimeRange {
        private Instant startTime;
        private Instant endTime;

        public Instant getStartTime() {
            return startTime;
        }

        public void setStartTime(Instant startTime) {
            this.startTime = startTime;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }
    }
}
