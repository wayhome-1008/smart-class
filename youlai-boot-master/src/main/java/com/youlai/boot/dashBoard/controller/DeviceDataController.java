package com.youlai.boot.dashBoard.controller;

import cn.idev.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.building.model.entity.Building;
import com.youlai.boot.building.service.BuildingService;
import com.youlai.boot.category.model.entity.Category;
import com.youlai.boot.category.service.CategoryService;
import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;
import com.youlai.boot.categoryDeviceRelationship.service.CategoryDeviceRelationshipService;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.InfluxQueryBuilder;
import com.youlai.boot.common.util.MathUtils;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.dashBoard.model.vo.*;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import com.youlai.boot.device.service.DeviceService;
import com.youlai.boot.floor.model.entity.Floor;
import com.youlai.boot.floor.service.FloorService;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.system.model.entity.Config;
import com.youlai.boot.system.model.entity.Dept;
import com.youlai.boot.system.service.ConfigService;
import com.youlai.boot.system.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.youlai.boot.common.util.MathUtils.formatDouble;

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
    private final BuildingService buildingService;
    private final FloorService floorService;

    @Operation(summary = "根据房间ID查询房间用电详情")
    @GetMapping("/room/electricity/detail")
    public Result<RoomElectricityDetailVO> getRoomElectricityDetail(
            @Parameter(description = "房间ID")
            @RequestParam Long roomId,

            @Parameter(description = "时间范围: today-今天/yesterday-昨天/week-本周/lastWeek-上周/month-本月/lastMonth-上月/custom-自定义")
            @RequestParam(defaultValue = "today") String range,

            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime) {

        try {
            // 获取房间的信息
            Room room = roomService.getById(roomId);
            if (room == null) {
                return Result.success(null);
            }

            // 获取部门信息
            String departmentName = "未知部门";
            if (room.getDepartmentId() != null) {
                Dept department = deptService.getById(room.getDepartmentId());
                if (department != null) {
                    departmentName = department.getName();
                }
            }
            // 获取楼宇和楼层信息（需要在Room实体中添加相关字段或通过关联查询获取）
            String buildingName = "未知楼宇";
            Building building = buildingService.getById(room.getBuildingId());
            if (building != null) {
                buildingName = building.getBuildingName();
            }
            String floorName = "未知楼层";
            Floor floor = floorService.getById(room.getFloorId());
            if (floor != null) {
                floorName = floor.getFloorNumber();
            }
            // 查询该房间下所有设备的分类用电量
            List<CategoryElectricityInfoVO> categoryElectricityList = getRoomCategoryElectricity(roomId, range, startTime, endTime);

            // 计算总用电量
            Double totalElectricity = categoryElectricityList.stream()
                    .mapToDouble(CategoryElectricityInfoVO::getTotalElectricity)
                    .sum();

            // 构造返回结果
            RoomElectricityDetailVO detailVO = new RoomElectricityDetailVO();
            detailVO.setRoomId(roomId);
            detailVO.setBuildingName(buildingName);
            detailVO.setFloorName(floorName);
            detailVO.setRoomName(room.getClassroomCode() != null ? room.getClassroomCode() : "未知房间");
            detailVO.setDepartmentName(departmentName);
            detailVO.setTotalElectricity(MathUtils.formatDouble(totalElectricity));

            // 转换分类用电量列表
            List<CategoryElectricityDataVO> categoryList = categoryElectricityList.stream()
                    .map(categoryInfo -> {
                        CategoryElectricityDataVO categoryVO = new CategoryElectricityDataVO();
                        categoryVO.setCategoryId(categoryInfo.getCategoryId());
                        categoryVO.setCategoryName(categoryInfo.getCategoryName());
                        categoryVO.setCategoryElectricity(categoryInfo.getTotalElectricity());
                        return categoryVO;
                    })
                    .collect(Collectors.toList());

            detailVO.setCategoryElectricityList(categoryList);

            return Result.success(detailVO);

        } catch (Exception e) {
            log.error("查询房间用电详情失败 - roomId: {}", roomId, e);
            return Result.failed("查询房间用电详情失败");
        }
    }

    /**
     * 查询指定房间下各分类的用电量
     */
    private List<CategoryElectricityInfoVO> getRoomCategoryElectricity(Long roomId, String range, String startTime, String endTime) {
        List<CategoryElectricityInfoVO> result = new ArrayList<>();

        try {
            // 1. 获取所有分类
            List<Category> categories = categoryService.list();
            if (ObjectUtils.isEmpty(categories)) {
                return result;
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

            // 3. 查询各分类用电量
            for (Category category : validCategories) {
                // 获取分类下的设备关系
                List<CategoryDeviceRelationship> relationships =
                        categoryDeviceRelationshipService.listByCategoryId(category.getId());

                if (!ObjectUtils.isEmpty(relationships)) {
                    List<Long> deviceIds = relationships.stream()
                            .map(CategoryDeviceRelationship::getDeviceId)
                            .collect(Collectors.toList());

                    // 筛选出该房间下的设备
                    List<Device> roomDevices = deviceService.listByIds(deviceIds).stream()
                            .filter(device -> device.getIsMaster() == 1 &&
                                    device.getDeviceRoom() != null &&
                                    device.getDeviceRoom().equals(roomId))
                            .toList();

                    if (!roomDevices.isEmpty()) {
                        // 计算该分类下该房间设备的总用电量
                        double categoryTotalElectricity = 0.0;
                        for (Device device : roomDevices) {
                            DeviceElectricityDataVO deviceData = getDeviceElectricityByRange(
                                    device.getDeviceCode(), range, startTime, endTime, roomId.toString());
                            if (deviceData != null && deviceData.getTotalElectricity() != null) {
                                categoryTotalElectricity += deviceData.getTotalElectricity();
                            }
                        }

                        if (categoryTotalElectricity > 0) {
                            CategoryElectricityInfoVO categoryVO = new CategoryElectricityInfoVO();
                            categoryVO.setCategoryId(category.getId());
                            categoryVO.setCategoryName(category.getCategoryName());
                            categoryVO.setTotalElectricity(MathUtils.formatDouble(categoryTotalElectricity));
                            categoryVO.setDeviceCount(roomDevices.size());
                            result.add(categoryVO);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("查询房间分类用电量失败 - roomId: {}, range: {}", roomId, range, e);
        }

        return result;
    }

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
     * 根据时间范围获取设备用电量（修改版）
     */
    private DeviceElectricityDataVO getDeviceElectricityByRange(String deviceCode, String range, String startTime, String endTime, String roomIds) {
        try {
            // 获取起始时间点的电表读数
            Double startTotal = getDeviceElectricityAtTime(deviceCode, startTime, roomIds, true);

            // 获取结束时间点的电表读数
            Double endTotal = getDeviceElectricityAtTime(deviceCode, endTime, roomIds, false);

            // 计算用电量差值
            Double totalElectricity = null;
            if (startTotal != null && endTotal != null) {
                totalElectricity = Math.max(0, endTotal - startTotal); // 确保不为负数
            } else if (startTotal == null && endTotal != null) {
                totalElectricity = endTotal; // 如果没有起始值，直接使用结束值
            } else if (startTotal != null && endTotal == null) {
                totalElectricity = startTotal; // 如果没有结束值，直接使用起始值
            }

            log.info("设备用电量计算 - 设备编码: {}, 起始电量: {}, 结束电量: {}, 差值: {}",
                    deviceCode, startTotal, endTotal, totalElectricity);

            return new DeviceElectricityDataVO(totalElectricity, Instant.now());
        } catch (Exception e) {
            log.error("计算设备用电量失败 - 设备编码: {}, 异常信息: ", deviceCode, e);
            return null;
        }
    }

    /**
     * 获取指定时间点的设备电表读数
     * @param deviceCode 设备编码
     * @param dateTime 时间字符串
     * @param roomIds 房间ID列表
     * @param isStart 是否为起始时间
     * @return 电表读数
     */
    private Double getDeviceElectricityAtTime(String deviceCode, String dateTime, String roomIds, boolean isStart) {
        try {
            if (StringUtils.isBlank(dateTime)) {
                // 如果时间为空，使用默认值
                Instant timeInstant = isStart ?
                        LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant() :
                        Instant.now(); // 结束时间使用当前时间

                return queryDeviceElectricity(deviceCode, timeInstant, roomIds);
            }

            // 解析日期时间
            Instant timeInstant = parseDateToInstant(dateTime, isStart);
            return queryDeviceElectricity(deviceCode, timeInstant, roomIds);
        } catch (Exception e) {
            log.error("获取设备电表读数失败 - 设备编码: {}, 时间: {}, 异常信息: ", deviceCode, dateTime, e);
            return null;
        }
    }

    /**
     * 查询指定时间点的设备电表读数
     * @param deviceCode 设备编码
     * @param timeInstant 时间点
     * @param roomIds 房间ID列表
     * @return 电表读数
     */
    private Double queryDeviceElectricity(String deviceCode, Instant timeInstant, String roomIds) {
        try {
            // 构建查询
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .tag("deviceCode", deviceCode)
                    .pivot()
                    .sort("_time", InfluxQueryBuilder.SORT_DESC)
                    .limit(1);

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

            // 设置时间范围查询 - 只查询指定时间点之前的数据
            if (timeInstant.equals(Instant.now())) {
                // 如果是当前时间，查询最近一天的数据
                builder.range(-1L, "d");
            } else {
                // 如果是指定时间点，查询从该时间点往前一天的数据，然后取最接近的那条
                builder.range(timeInstant.minusSeconds(86400), timeInstant); // 查询前24小时到指定时间点
            }

            String fluxQuery = builder.build();
            log.info("查询设备电表读数 - 设备编码: {}, 时间点: {}, 查询语句: {}", deviceCode, timeInstant, fluxQuery);

            List<InfluxMqttPlug> tables = influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            if (!tables.isEmpty()) {
                InfluxMqttPlug result = tables.get(0);
                return result.getTotal();
            }

            return null;
        } catch (Exception e) {
            log.error("查询设备电表读数失败 - 设备编码: {}, 时间点: {}, 异常信息: ", deviceCode, timeInstant, e);
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

    @Operation(summary = "分页查询各房间总用电量")
    @GetMapping("/room/electricity")
    public PageResult<RoomsElectricityVO> getRoomElectricityPage(
            @Parameter(description = "页码，默认为1")
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页数量，默认为10")
            @RequestParam(defaultValue = "10") Integer pageSize,

            @Parameter(description = "房间ID")
            @RequestParam(required = false) String roomIds,

            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime) {

        return getRoomsElectricityVOPageResult(pageNum, pageSize, roomIds, startTime, endTime);
    }

    @NotNull
    private PageResult<RoomsElectricityVO> getRoomsElectricityVOPageResult(Integer pageNum, Integer pageSize, String roomIds, String startTime, String endTime) {
        try {
            if (StringUtils.isEmpty(roomIds)) {
                List<Room> roomList = roomService.list(new LambdaQueryWrapper<Room>().eq(Room::getIsDeleted, 0));
                if (ObjectUtils.isEmpty(roomList)) {
                    return PageResult.success(new Page<>());
                }
                roomIds = roomList.stream().map(Room::getId).map(String::valueOf).collect(Collectors.joining(","));
            }
            List<RoomsElectricityVO> roomsElectricityList = getRoomsElectricity(startTime, endTime, roomIds);

            // 按用电量降序排列
            roomsElectricityList.sort((a, b) -> Double.compare(b.getTotalElectricity(), a.getTotalElectricity()));

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

    @Operation(summary = "导出各房间总用电量")
    @GetMapping("/export/room/electricity")
    public void exportRoomElectricity(@Parameter(description = "房间ID")
                                      @RequestParam(required = false) String roomIds,

                                      @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
                                      @RequestParam(required = false) String startTime,

                                      @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
                                      @RequestParam(required = false) String endTime, HttpServletResponse response) throws IOException {
        String fileName = "房间总用电量.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        PageResult<RoomsElectricityVO> roomsElectricityVOPageResult = getRoomsElectricityVOPageResult(1, 999999, roomIds, startTime, endTime);
        List<RoomsElectricityVO> list = roomsElectricityVOPageResult.getData().getList();
        EasyExcel.write(response.getOutputStream(), RoomsElectricityVO.class).sheet("房间总用电量")
                .doWrite(list);
    }

    /**
     * 根据时间范围获取房间用电量（修改版）
     */
    private List<RoomsElectricityVO> getRoomsElectricity(String startTime, String endTime, String roomIds) {
        // 查询所有房间
        List<Room> roomList = roomService.list(new QueryWrapper<Room>().in("id", Arrays.stream(roomIds.split(","))
                .map(Long::parseLong)
                .toList()));

        // 获取起始时间点的电表读数
        List<InfluxMqttPlug> startTables = queryRoomsElectricityAtTime(startTime, roomIds, true);

        // 获取结束时间点的电表读数
        List<InfluxMqttPlug> endTables = queryRoomsElectricityAtTime(endTime, roomIds, false);

        List<RoomsElectricityVO> resultList = new ArrayList<>();
        for (Room room : roomList) {
            RoomsElectricityVO vo = new RoomsElectricityVO();
            vo.setRoomId(room.getId());
            vo.setRoomName(room.getClassroomCode());

            // 查找起始时间点的读数
            Optional<InfluxMqttPlug> startData = startTables.stream()
                    .filter(table -> table.getRoomId() != null &&
                            table.getRoomId().equals(String.valueOf(room.getId())))
                    .findFirst();

            // 查找结束时间点的读数
            Optional<InfluxMqttPlug> endData = endTables.stream()
                    .filter(table -> table.getRoomId() != null &&
                            table.getRoomId().equals(String.valueOf(room.getId())))
                    .findFirst();

            // 计算用电量差值
            Double totalElectricity = null;
            Double startTotal = startData.isPresent() ? startData.get().getTotal() : null;
            Double endTotal = endData.isPresent() ? endData.get().getTotal() : null;

            if (startTotal != null && endTotal != null) {
                totalElectricity = Math.max(0, endTotal - startTotal); // 确保不为负数
            } else if (startTotal == null && endTotal != null) {
                totalElectricity = endTotal; // 如果没有起始值，直接使用结束值
            } else if (startTotal != null && endTotal == null) {
                totalElectricity = startTotal; // 如果没有结束值，直接使用起始值
            } else {
                totalElectricity = 0.0; // 都没有数据则为0
            }

            vo.setTotalElectricity(totalElectricity);
            resultList.add(vo);
        }
        return resultList;
    }

    /**
     * 查询指定时间点的房间电表读数
     * @param dateTime 时间字符串
     * @param roomIds 房间ID列表
     * @param isStart 是否为起始时间
     * @return 电表读数列表
     */
    private List<InfluxMqttPlug> queryRoomsElectricityAtTime(String dateTime, String roomIds, boolean isStart) {
        try {
            // 根据时间范围构建查询
            InfluxQueryBuilder builder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("Total")
                    .pivot()
                    .sort("_time", InfluxQueryBuilder.SORT_DESC)
                    .limit(1);

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

            // 设置时间范围查询 - 只查询指定时间点之前的数据
            if (timeInstant.equals(Instant.now())) {
                // 如果是当前时间，查询最近一天的数据
                builder.range(-1L, "d");
            } else {
                // 如果是指定时间点，查询从该时间点往前一天的数据
                builder.range(timeInstant.minusSeconds(86400), timeInstant);
            }

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


    @Operation(summary = "分页查询各部门总用电量")
    @GetMapping("/department/electricity")
    public PageResult<DepartmentElectricityVO> getDepartmentElectricityPage(
            @Parameter(description = "页码，默认为1")
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页数量，默认为10")
            @RequestParam(defaultValue = "10") Integer pageSize,

            @Parameter(description = "部门IDs，多个用逗号分隔")
            @RequestParam(required = false) String deptIds,

            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime) {
        return getDepartmentElectricityVOPageResult(pageNum, pageSize, deptIds, startTime, endTime);
    }

    @Operation(summary = "导出各部门总用电量")
    @GetMapping("/export/department/electricity")
    public void exportDepartmentElectricity(@Parameter(description = "部门IDs，多个用逗号分隔")
                                            @RequestParam(required = false) String deptIds,

                                            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
                                            @RequestParam(required = false) String startTime,

                                            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
                                            @RequestParam(required = false) String endTime, HttpServletResponse response) throws IOException {
        String fileName = "部门总用电量.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        PageResult<DepartmentElectricityVO> departmentElectricityVOPageResult = getDepartmentElectricityVOPageResult(1, 999999, deptIds, startTime, endTime);
        List<DepartmentElectricityVO> list = departmentElectricityVOPageResult.getData().getList();
        EasyExcel.write(response.getOutputStream(), DepartmentElectricityVO.class).sheet("部门总用电量")
                .doWrite(list);
    }

    @NotNull
    private PageResult<DepartmentElectricityVO> getDepartmentElectricityVOPageResult(Integer pageNum, Integer pageSize, String deptIds, String startTime, String endTime) {
        List<Long> deptIdList;
        try {
            if (StringUtils.isBlank(deptIds)) {
                List<Dept> list = deptService.list();
                if (list.isEmpty()) {
                    Page<DepartmentElectricityVO> page = new Page<>(pageNum, pageSize);
                    page.setTotal(0);
                    page.setRecords(new ArrayList<>());
                    return PageResult.success(page);
                }
                deptIds = list.stream().map(Dept::getId).map(String::valueOf).collect(Collectors.joining(","));
            }
            // 解析部门ID列表
            deptIdList = Arrays.stream(deptIds.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(Long::parseLong)
                    .toList();
            // 根据部门ID查询房间列表
            List<Room> allRooms = roomService.list(new QueryWrapper<Room>().in("department_id", deptIdList));

            if (allRooms.isEmpty()) {
                Page<DepartmentElectricityVO> page = new Page<>(pageNum, pageSize);
                page.setTotal(0);
                page.setRecords(new ArrayList<>());
                return PageResult.success(page);
            }

            // 按部门ID分组房间
            Map<Long, List<Room>> roomsByDept = allRooms.stream()
                    .collect(Collectors.groupingBy(Room::getDepartmentId));

            // 获取所有房间ID
            List<Long> roomIds = allRooms.stream()
                    .map(Room::getId)
                    .toList();

            // 将房间ID列表转换为字符串格式
            String roomIdsStr = roomIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            // 查询所有房间的用电量数据
            List<RoomsElectricityVO> roomsElectricityList = getRoomsElectricity(startTime, endTime, roomIdsStr);

            // 构建部门用电量结果
            List<DepartmentElectricityVO> result = new ArrayList<>();

            // 按部门分组计算用电量
            for (Map.Entry<Long, List<Room>> entry : roomsByDept.entrySet()) {
                Long departmentId = entry.getKey();
                List<Room> roomsInDept = entry.getValue();

                // 获取部门信息
                Dept department = deptService.getById(departmentId);
                if (department == null) {
                    continue;
                }

                // 获取该部门下所有房间的用电量
                List<Long> roomIdsInDept = roomsInDept.stream()
                        .map(Room::getId)
                        .toList();

                // 计算该部门总用电量
                double totalElectricity = roomsElectricityList.stream()
                        .filter(vo -> roomIdsInDept.contains(vo.getRoomId()) && vo.getTotalElectricity() != null)
                        .mapToDouble(RoomsElectricityVO::getTotalElectricity)
                        .sum();

                // 构造返回结果
                DepartmentElectricityVO deptElectricityVO = new DepartmentElectricityVO();
                deptElectricityVO.setDepartmentId(departmentId);
                deptElectricityVO.setDepartmentName(department.getName());
                deptElectricityVO.setTotalElectricity(formatDouble(totalElectricity));

                result.add(deptElectricityVO);
            }

            // 按用电量降序排列
            result.sort((a, b) -> Double.compare(b.getTotalElectricity(), a.getTotalElectricity()));

            // 分页处理
            int total = result.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            List<DepartmentElectricityVO> pagedResult = new ArrayList<>();
            if (fromIndex < total) {
                pagedResult = result.subList(fromIndex, toIndex);
            }

            // 构造分页结果
            Page<DepartmentElectricityVO> page = new Page<>(pageNum, pageSize);
            page.setTotal(total);
            page.setRecords(pagedResult);

            return PageResult.success(page);

        } catch (Exception e) {
            log.error("分页查询各部门用电量失败 - deptIds: {}", deptIds, e);
            return PageResult.success(null);
        }
    }


}
