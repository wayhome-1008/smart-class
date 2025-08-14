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
import com.youlai.boot.dashBoard.model.vo.DepartmentCategoryElectricityInfoVO;
import com.youlai.boot.dashBoard.model.vo.DeviceElectricityDataVO;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.youlai.boot.common.util.DateUtils.formatTime;

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
                        DeviceElectricityDataVO deviceData = getDeviceElectricityByRange(masterDevice.getDeviceCode(), range, startTime, endTime, roomIds);
                        if (deviceData != null && deviceData.getTotalElectricity() != null) {
                            // 获取设备所在房间
                            Room room = roomMap.get(masterDevice.getDeviceRoom());
                            if (room != null) {
                                Long departmentId = room.getDepartmentId();
                                Dept department = deptService.getById(departmentId);
                                String departmentName = department.getName();

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
                                    vo.setRoomName(room.getClassroomCode() != null ? room.getClassroomCode() : "未知房间");
                                    vo.setRoomId(room.getId());
                                    departmentCategoryMap.put(groupKey, vo);
                                }
                                // 累加用电量和设备数量
                                vo.setTotalElectricity(MathUtils.formatDouble(vo.getTotalElectricity() + deviceData.getTotalElectricity()));
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

    /**
     * 根据时间范围获取设备用电量
     */
    private DeviceElectricityDataVO getDeviceElectricityByRange(String deviceCode, String range, String startTime, String endTime, String roomIds) {
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
            // 使用自定义时间范围
            if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
                // 解析日期字符串并转换为Instant
                Instant startInstant = parseDateToInstant(startTime, true);  // true表示开始时间(00:00:00)
                Instant endInstant = parseDateToInstant(endTime, false);     // false表示结束时间(23:59:59)
                builder.range(startInstant, endInstant);
            } else if (StringUtils.isNotBlank(startTime)) {
                // 只有开始时间
                Instant startInstant = parseDateToInstant(startTime, true);
                // 结束时间默认为今天
                Instant endInstant = LocalDate.now().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
                builder.range(startInstant, endInstant);
            } else if (StringUtils.isNotBlank(endTime)) {
                // 只有结束时间
                // 开始时间默认为7天前
                Instant startInstant = LocalDate.now().minusDays(7).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
                Instant endInstant = parseDateToInstant(endTime, false);
                builder.range(startInstant, endInstant);
            } else {
                // 默认使用今天的时间范围
                Instant startInstant = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
                Instant endInstant = LocalDate.now().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
                builder.range(startInstant, endInstant);
            }

            // 构建并打印查询语句
            String fluxQuery = builder.build();
            log.info("InfluxDB查询 - 设备编码: {}, 时间范围: {} 至 {}, 房间IDs: {}, 查询语句: {}",
                    deviceCode, startTime, endTime, roomIds, fluxQuery);

            List<InfluxMqttPlug> tables = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (!tables.isEmpty()) {
                InfluxMqttPlug result = tables.get(0);
                Double total = result.getTotal();
                Instant createTime = result.getTime(); // 获取数据的创建时间

                log.info("InfluxDB查询成功 - 设备编码: {}, 时间范围: {} 至 {}, 查询结果: {}, 创建时间: {}",
                        deviceCode, startTime, endTime, total, createTime);

                return new DeviceElectricityDataVO(total, createTime);
            } else {
                log.info("InfluxDB查询无数据 - 设备编码: {}, 时间范围: {} 至 {}",
                        deviceCode, startTime, endTime);
                return null;
            }
        } catch (Exception e) {
            log.error("InfluxDB查询失败 - 设备编码: {}, 时间范围: {} 至 {}, 异常信息: ",
                    deviceCode, startTime, endTime, e);
            return null;
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
}
