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
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.util.InfluxQueryBuilder;
import com.youlai.boot.common.util.MathUtils;
import com.youlai.boot.config.property.InfluxDBProperties;
import com.youlai.boot.dashBoard.model.vo.*;
import com.youlai.boot.dashBoard.service.ElectricityCalculationService;
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
import lombok.Getter;
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
 * @Author: way
 * @CreateTime: 2025-08-11  15:30
 * @Description: 数据汇总接口
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
    private final ElectricityCalculationService electricityCalculationService;

    @Operation(summary = "设备下拉列表")
    @GetMapping("/options/categoryName")
    public Result<List<Option<String>>> listCategoryNameOptions() {
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
        List<Option<String>> list1 = new ArrayList<>(categoriesAll.size());
        for (String name : categoryNames) {
            Option<String> option = new Option<>();
            option.setValue(name);
            option.setLabel(name);
            list1.add(option);
        }
        return Result.success(list1);
    }


    @Operation(summary = "1.部门明细")
    @GetMapping("/department/electricity/detail")
    public Result<DepartmentElectricityDetailVO> getDepartmentElectricityDetail(
            @Parameter(description = "部门ID")
            @RequestParam Long departmentId,

            @Parameter(description = "时间范围: today-今天/yesterday-昨天/week-本周/lastWeek-上周/month-本月/lastMonth-上月/custom-自定义")
            @RequestParam(defaultValue = "today") String range,

            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime) {

        try {
            // 获取部门信息
            Dept department = deptService.getById(departmentId);
            if (department == null) {
                return Result.success(null);
            }

            // 查询该部门下所有房间
            List<Room> rooms = roomService.list(new LambdaQueryWrapper<Room>()
                    .eq(Room::getDepartmentId, departmentId)
                    .eq(Room::getIsDeleted, 0));

            if (ObjectUtils.isEmpty(rooms)) {
                DepartmentElectricityDetailVO detailVO = new DepartmentElectricityDetailVO();
                detailVO.setDepartmentId(departmentId);
                detailVO.setDepartmentName(department.getName());
                detailVO.setTotalElectricity(0.0);
                detailVO.setRoomElectricityList(new ArrayList<>());
                return Result.success(detailVO);
            }

            // 计算部门总用电量
            double departmentTotalElectricity = 0.0;

            // 构建房间用电量列表
            List<RoomElectricityDataVO> roomElectricityList = new ArrayList<>();

            // 查询每个房间的用电情况
            for (Room room : rooms) {
                // 查询该房间下所有设备的分类用电量
                List<CategoryElectricityInfoVO> categoryElectricityList = getRoomCategoryElectricity(
                        room.getId(), range, startTime, endTime);

                // 计算房间总用电量
                double roomTotalElectricity = categoryElectricityList.stream()
                        .mapToDouble(CategoryElectricityInfoVO::getTotalElectricity)
                        .sum();
                // 获取楼宇信息
                String buildingName = "未知楼宇";
                if (room.getBuildingId() != null) {
                    Building building = buildingService.getById(room.getBuildingId());
                    if (building != null) {
                        buildingName = building.getBuildingName();
                    }
                }

                // 获取楼层信息
                String floorName = "未知楼层";
                if (room.getFloorId() != null) {
                    Floor floor = floorService.getById(room.getFloorId());
                    if (floor != null) {
                        floorName = floor.getFloorNumber();
                    }
                }
                departmentTotalElectricity += roomTotalElectricity;

                // 构建房间用电详情
                RoomElectricityDataVO roomVO = new RoomElectricityDataVO();
                roomVO.setRoomId(room.getId());
                roomVO.setRoomName(room.getClassroomCode() != null ? room.getClassroomCode() : "未知房间");
                roomVO.setBuildingName(buildingName);
                roomVO.setFloorName(floorName);
                roomVO.setTotalElectricity(MathUtils.formatDouble(roomTotalElectricity));

                // 转换分类用电量列表
                List<CategoryElectricityDataVO> categoryList = categoryElectricityList.stream()
                        .map(categoryInfo -> {
                            CategoryElectricityDataVO categoryVO = new CategoryElectricityDataVO();
                            categoryVO.setCategoryId(categoryInfo.getCategoryId());
                            categoryVO.setCategoryName(categoryInfo.getCategoryName());
                            categoryVO.setCategoryElectricity(categoryInfo.getTotalElectricity());

                            // 设置开关时间
                            categoryVO.setStartTime(categoryInfo.getStartTime());
                            categoryVO.setEndTime(categoryInfo.getEndTime());

                            return categoryVO;
                        })
                        .collect(Collectors.toList());

                roomVO.setCategoryElectricityList(categoryList);
                roomElectricityList.add(roomVO);
            }

            // 构造返回结果
            DepartmentElectricityDetailVO detailVO = new DepartmentElectricityDetailVO();
            detailVO.setDepartmentId(departmentId);
            detailVO.setDepartmentName(department.getName());
            detailVO.setTotalElectricity(MathUtils.formatDouble(departmentTotalElectricity));
            detailVO.setRoomElectricityList(roomElectricityList);

            return Result.success(detailVO);

        } catch (Exception e) {
            log.error("查询部门用电详情失败 - departmentId: {}, range: {}", departmentId, range, e);
            return Result.failed("查询部门用电详情失败");
        }
    }

    @Operation(summary = "2.房间明细")
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

                        // 直接设置开关时间字符串
                        categoryVO.setStartTime(categoryInfo.getStartTime());
                        categoryVO.setEndTime(categoryInfo.getEndTime());

                        return categoryVO;
                    })
                    .collect(Collectors.toList());

            detailVO.setCategoryElectricityList(categoryList);

            return Result.success(detailVO);

        } catch (Exception e) {
            log.error("查询房间用电详情失败 - roomId: {}, range: {}", roomId, range, e);
            return Result.failed("查询房间用电详情失败");
        }
    }


    @Operation(summary = "3.部门/楼宇汇总分页")
    @GetMapping("/electricity/page")
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
            return getDepartmentCategoryElectricityInfoVOPageResult(pageNum, pageSize, range, startTime, endTime, deptIds, roomIds);
        } catch (Exception e) {
            log.error("分页查询各部门各分类用电量失败: ", e);
        }
        return PageResult.success(null);
    }


    @Operation(summary = "4.楼宇电量汇总导出")
    @GetMapping("/export/room/electricity")
    public void exportRoomElectricity(@Parameter(description = "页码，默认为1")
                                      @RequestParam(defaultValue = "1") Integer pageNum,

                                      @Parameter(description = "每页数量，默认为10")
                                      @RequestParam(defaultValue = "10") Integer pageSize,

                                      @Parameter(description = "时间范围: today-今天/yesterday-昨天/week-本周/lastWeek-上周/month-本月/lastMonth-上月/custom-自定义")
                                      @RequestParam(defaultValue = "today") String range,

                                      @Parameter(description = "房间ID")
                                      @RequestParam(required = false) String roomIds,

                                      @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
                                      @RequestParam(required = false) String startTime,

                                      @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
                                      @RequestParam(required = false) String endTime,
                                      HttpServletResponse response) throws IOException {
        String fileName = "楼宇电量汇总.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        PageResult<DepartmentCategoryElectricityInfoVO> departmentElectricityVOPageResult = getDepartmentCategoryElectricityInfoVOPageResult(pageNum, pageSize, range, startTime, endTime, null, roomIds);
        List<DepartmentCategoryElectricityInfoVO> list = departmentElectricityVOPageResult.getData().getList();
        List<RoomsElectricityVO> roomsElectricityVOList = new ArrayList<>();
        for (DepartmentCategoryElectricityInfoVO departmentCategoryElectricityInfoVO : list) {
            RoomsElectricityVO roomsElectricityVO = new RoomsElectricityVO();
            roomsElectricityVO.setRoomId(departmentCategoryElectricityInfoVO.getRoomId());
            roomsElectricityVO.setRoomName(departmentCategoryElectricityInfoVO.getRoomName());
            roomsElectricityVO.setTotalElectricity(departmentCategoryElectricityInfoVO.getTotalElectricity());
            roomsElectricityVO.setCategoryName(departmentCategoryElectricityInfoVO.getCategoryName());
            roomsElectricityVOList.add(roomsElectricityVO);
        }
        EasyExcel.write(response.getOutputStream(), RoomsElectricityVO.class).sheet("楼宇电量汇总")
                .doWrite(roomsElectricityVOList);
    }

    @Operation(summary = "5.部门电量汇总导出")
    @GetMapping("/export/department/electricity")
    public void exportDepartmentElectricity(@Parameter(description = "页码，默认为1")
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
                                            @RequestParam(required = false) String roomIds
            , HttpServletResponse response) throws IOException {
        String fileName = "部门电量汇总.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        PageResult<DepartmentCategoryElectricityInfoVO> departmentElectricityVOPageResult = getDepartmentCategoryElectricityInfoVOPageResult(pageNum, pageSize, range, startTime, endTime, deptIds, roomIds);
        List<DepartmentCategoryElectricityInfoVO> list = departmentElectricityVOPageResult.getData().getList();
        List<ExportDepartment> exportDepartmentList = new ArrayList<>();
        for (DepartmentCategoryElectricityInfoVO departmentCategoryElectricityInfoVO : list) {
            ExportDepartment exportDepartment = new ExportDepartment();
            exportDepartment.setDepartmentId(departmentCategoryElectricityInfoVO.getDepartmentId());
            exportDepartment.setDepartmentName(departmentCategoryElectricityInfoVO.getDepartmentName());
            exportDepartment.setTotalElectricity(departmentCategoryElectricityInfoVO.getTotalElectricity());
            exportDepartment.setCategoryName(departmentCategoryElectricityInfoVO.getCategoryName());
            exportDepartmentList.add(exportDepartment);
        }
        EasyExcel.write(response.getOutputStream(), ExportDepartment.class).sheet("部门电量汇总")
                .doWrite(exportDepartmentList);
    }

    /**
     * 查询设备的最早开启时间和最晚关闭时间
     *
     * @param deviceCode 设备编码
     * @param range      时间范围 (today/yesterday)
     * @return 包含最早开启时间和最晚关闭时间的对象
     */
    private SwitchTimeInfo queryDeviceSwitchTimes(String deviceCode, String range) {
        try {
            Instant startTime, endTime;

            if ("today".equals(range)) {
                startTime = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
                endTime = Instant.now();
            } else if ("yesterday".equals(range)) {
                startTime = LocalDate.now().minusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
                endTime = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            } else {
                return new SwitchTimeInfo(null, null);
            }

            // 查询最早开启时间 (switch为on的最早记录)
            InfluxQueryBuilder onBuilder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("switch")
                    .tag("deviceCode", deviceCode)
                    .pivot()
                    .filter("r._value == \"ON\"")
                    .sort("_time", InfluxQueryBuilder.SORT_ASC)
                    .limit(1)
                    .range(startTime, endTime);

            String onFluxQuery = onBuilder.build();
            log.info("查询设备最早开启时间 - 设备编码: {}, 查询语句: {}", deviceCode, onFluxQuery);

            List<InfluxMqttPlug> onResults = influxDBClient.getQueryApi()
                    .query(onFluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            Instant earliestOnTime = null;
            if (!onResults.isEmpty()) {
                earliestOnTime = onResults.get(0).getTime();
            }

            // 查询最晚关闭时间 (switch为off的最晚记录)
            InfluxQueryBuilder offBuilder = InfluxQueryBuilder.newBuilder()
                    .bucket(influxDBProperties.getBucket())
                    .measurement("device")
                    .fields("switch")
                    .tag("deviceCode", deviceCode)
                    .pivot()
                    .filter("r._value == \"OFF\"")
                    .sort("_time", InfluxQueryBuilder.SORT_DESC)
                    .limit(1)
                    .range(startTime, endTime);

            String offFluxQuery = offBuilder.build();
            log.debug("查询设备最晚关闭时间 - 设备编码: {}, 查询语句: {}", deviceCode, offFluxQuery);

            List<InfluxMqttPlug> offResults = influxDBClient.getQueryApi()
                    .query(offFluxQuery, influxDBProperties.getOrg(), InfluxMqttPlug.class);

            Instant latestOffTime = null;
            if (!offResults.isEmpty()) {
                latestOffTime = offResults.get(0).getTime();
            }

            return new SwitchTimeInfo(earliestOnTime, latestOffTime);

        } catch (Exception e) {
            log.error("查询设备开关时间失败 - 设备编码: {}, 时间范围: {}", deviceCode, range, e);
            return new SwitchTimeInfo(null, null);
        }
    }

    /**
     * 用于存储开关时间信息的简单类
     */
    @Getter
    private static class SwitchTimeInfo {
        private final Instant earliestOnTime;
        private final Instant latestOffTime;

        public SwitchTimeInfo(Instant earliestOnTime, Instant latestOffTime) {
            this.earliestOnTime = earliestOnTime;
            this.latestOffTime = latestOffTime;
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
                        Instant categoryEarliestOnTime = null;
                        Instant categoryLatestOffTime = null;

                        // 为昨天或今天查询时，收集开关时间信息
                        if ("today".equals(range) || "yesterday".equals(range)) {
                            List<Instant> onTimes = new ArrayList<>();
                            List<Instant> offTimes = new ArrayList<>();

                            for (Device device : roomDevices) {
                                SwitchTimeInfo switchTimeInfo = queryDeviceSwitchTimes(
                                        device.getDeviceCode(), range);

                                if (switchTimeInfo.getEarliestOnTime() != null) {
                                    onTimes.add(switchTimeInfo.getEarliestOnTime());
                                }

                                if (switchTimeInfo.getLatestOffTime() != null) {
                                    offTimes.add(switchTimeInfo.getLatestOffTime());
                                }

                                DeviceElectricityDataVO deviceData = getDeviceElectricityByRange(
                                        device.getDeviceCode(), range, startTime, endTime, roomId.toString());
                                if (deviceData != null && deviceData.getTotalElectricity() != null) {
                                    categoryTotalElectricity += deviceData.getTotalElectricity();
                                }
                            }

                            // 从所有设备中选择最早的开启时间和最晚的关闭时间
                            if (!onTimes.isEmpty()) {
                                categoryEarliestOnTime = onTimes.stream()
                                        .min(Instant::compareTo)
                                        .orElse(null);
                            }

                            if (!offTimes.isEmpty()) {
                                categoryLatestOffTime = offTimes.stream()
                                        .max(Instant::compareTo)
                                        .orElse(null);
                            }
                        } else {
                            // 非今天或昨天的查询，只计算用电量
                            for (Device device : roomDevices) {
                                DeviceElectricityDataVO deviceData = getDeviceElectricityByRange(
                                        device.getDeviceCode(), range, startTime, endTime, roomId.toString());
                                if (deviceData != null && deviceData.getTotalElectricity() != null) {
                                    categoryTotalElectricity += deviceData.getTotalElectricity();
                                }
                            }
                        }

                        if (categoryTotalElectricity > 0 || categoryEarliestOnTime != null || categoryLatestOffTime != null) {
                            CategoryElectricityInfoVO categoryVO = new CategoryElectricityInfoVO();
                            categoryVO.setCategoryId(category.getId());
                            categoryVO.setCategoryName(category.getCategoryName());
                            categoryVO.setTotalElectricity(MathUtils.formatDouble(categoryTotalElectricity));
                            categoryVO.setDeviceCount(roomDevices.size());

                            // 设置开关时间信息
                            if (categoryEarliestOnTime != null) {
                                String formattedStartTime = categoryEarliestOnTime
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                categoryVO.setStartTime(formattedStartTime);
                            }
                            if (categoryLatestOffTime != null) {
                                String formattedEndTime = categoryLatestOffTime
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                categoryVO.setEndTime(formattedEndTime);
                            }

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

    @NotNull
    private PageResult<DepartmentCategoryElectricityInfoVO> getDepartmentCategoryElectricityInfoVOPageResult(
            Integer pageNum, Integer pageSize, String range, String startTime, String endTime,
            String deptIds, String roomIds) {
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
        QueryWrapper<Room> roomQueryWrapper = new QueryWrapper<>();

        // 根据是否有部门ID或房间ID来决定查询条件
        boolean hasDeptIds = StringUtils.isNotBlank(deptIds);
        boolean hasRoomIds = StringUtils.isNotBlank(roomIds);

        if (hasDeptIds) {
            // 部门汇总：根据部门ID查询
            List<Long> deptIdList = Arrays.stream(deptIds.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(Long::parseLong)
                    .toList();
            roomQueryWrapper.in("department_id", deptIdList);
        } else if (hasRoomIds) {
            // 楼宇汇总：根据房间ID查询
            List<Long> roomIdList = Arrays.stream(roomIds.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(Long::parseLong)
                    .toList();
            roomQueryWrapper.in("id", roomIdList);
        }

        List<Room> allRooms = roomService.list(roomQueryWrapper);
        if (ObjectUtils.isEmpty(allRooms)) {
            Page<DepartmentCategoryElectricityInfoVO> page = new Page<>(pageNum, pageSize);
            page.setTotal(0);
            page.setRecords(new ArrayList<>());
            return PageResult.success(page);
        }

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
                    // 检查设备是否属于查询范围内的房间
                    if (roomMap.containsKey(masterDevice.getDeviceRoom())) {
                        DeviceElectricityDataVO deviceData = getDeviceElectricityByRange(
                                masterDevice.getDeviceCode(), range, startTime, endTime, roomIds);

                        if (deviceData != null && deviceData.getTotalElectricity() != null) {
                            // 获取设备所在房间
                            Room room = roomMap.get(masterDevice.getDeviceRoom());
                            if (room != null) {
                                Long departmentId = room.getDepartmentId();
                                Dept department = deptService.getById(departmentId);
                                String departmentName = department != null ? department.getName() : "未知部门";

                                // 构造分组键：部门ID_分类ID
                                String groupKey = departmentId + "_" + category.getId();

                                DepartmentCategoryElectricityInfoVO vo = departmentCategoryMap.get(groupKey);
                                if (vo == null) {
                                    vo = new DepartmentCategoryElectricityInfoVO();
                                    vo.setDepartmentId(departmentId);
                                    vo.setDepartmentName(departmentName);
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
        }

        departmentCategoryElectricityList.addAll(departmentCategoryMap.values());

        // 按部门ID分组，计算每个部门的总用电量
        Map<Long, Double> departmentTotalElectricityMap = departmentCategoryElectricityList.stream()
                .collect(Collectors.groupingBy(
                        DepartmentCategoryElectricityInfoVO::getDepartmentId,
                        Collectors.summingDouble(DepartmentCategoryElectricityInfoVO::getTotalElectricity)
                ));

        // 为每个记录设置分类总用电量和占比
        for (DepartmentCategoryElectricityInfoVO item : departmentCategoryElectricityList) {
            Double departmentTotal = formatDouble(departmentTotalElectricityMap.get(item.getDepartmentId()));
            if (departmentTotal != null && departmentTotal > 0) {
                item.setDepartmentTotalElectricity(departmentTotal);
            } else {
                item.setDepartmentTotalElectricity(0.0);
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
    }


    /**
     * 根据时间范围获取设备用电量（修改版）
     */
    private DeviceElectricityDataVO getDeviceElectricityByRange(String deviceCode, String range, String startTime, String endTime, String roomIds) {
        try {
            Double totalElectricity = electricityCalculationService.calculateDeviceElectricity(deviceCode, range, startTime, endTime, roomIds);
            log.info("设备用电量计算 - 设备编码: {}, 范围: {}, 用电量: {}", deviceCode, range, totalElectricity);
            return new DeviceElectricityDataVO(totalElectricity, Instant.now());
        } catch (Exception e) {
            log.error("计算设备用电量失败 - 设备编码: {}, 范围: {}, 异常信息: ", deviceCode, range, e);
            return null;
        }
    }

    @Operation(summary = "6.楼宇排名导出")
    @GetMapping("/export/room/rank")
    public void exportRoomRanking(
            @Parameter(description = "页码，默认为1")
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页数量，默认为10")
            @RequestParam(defaultValue = "10") Integer pageSize,

            @Parameter(description = "时间范围: today-今天/yesterday-昨天/week-本周/lastWeek-上周/month-本月/lastMonth-上月/custom-自定义")
            @RequestParam(defaultValue = "today") String range,

            @Parameter(description = "房间ID")
            @RequestParam(required = false) String roomIds,
            @Parameter(description = "分类名称")
            @RequestParam(required = false) String categoryName,

            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime,
            HttpServletResponse response
    ) throws IOException {
        String fileName = "楼宇排名.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        PageResult<RoomsElectricityVO> roomsElectricityVOPageResult = electricityCalculationService.getRoomsElectricityVOPageResult(pageNum, pageSize, roomIds, startTime, endTime, range, categoryName);
        List<RoomsElectricityVO> list = roomsElectricityVOPageResult.getData().getList();
        List<ExportRoomRank> exportRoomRanksList = new ArrayList<>();
        for (RoomsElectricityVO electricityVO : list) {
            ExportRoomRank exportRoomRank = new ExportRoomRank();
            exportRoomRank.setRoomName(electricityVO.getRoomName());
            exportRoomRank.setTotalElectricity(electricityVO.getTotalElectricity());
            exportRoomRank.setCategoryName(electricityVO.getCategoryName());
            exportRoomRanksList.add(exportRoomRank);
        }
        EasyExcel.write(response.getOutputStream(), ExportRoomRank.class).sheet("楼宇排名")
                .doWrite(exportRoomRanksList);

    }

    @Operation(summary = "7.楼宇排名")
    @GetMapping("/room/electricity/rank")
    public PageResult<RoomsElectricityVO> getRoomRankingPage(
            @Parameter(description = "页码，默认为1")
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页数量，默认为10")
            @RequestParam(defaultValue = "10") Integer pageSize,

            @Parameter(description = "时间范围: today-今天/yesterday-昨天/week-本周/lastWeek-上周/month-本月/lastMonth-上月/custom-自定义")
            @RequestParam(defaultValue = "today") String range,

            @Parameter(description = "房间ID")
            @RequestParam(required = false) String roomIds,
            @Parameter(description = "分类名称")
            @RequestParam(required = false) String categoryName,

            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime) {

        return electricityCalculationService.getRoomsElectricityVOPageResult(pageNum, pageSize, roomIds, startTime, endTime, range, categoryName);
    }

    @Operation(summary = "8.部门排名")
    @GetMapping("/department/electricity/rank")
    public PageResult<DepartmentElectricityVO> getDepartmentRankingPage(
            @Parameter(description = "页码，默认为1")
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页数量，默认为10")
            @RequestParam(defaultValue = "10") Integer pageSize,

            @Parameter(description = "时间范围: today-今天/yesterday-昨天/week-本周/lastWeek-上周/month-本月/lastMonth-上月/custom-自定义")
            @RequestParam(defaultValue = "today") String range,

            @Parameter(description = "部门IDs，多个用逗号分隔")
            @RequestParam(required = false) String deptIds,

            @Parameter(description = "分类名称")
            @RequestParam(required = false) String categoryName,

            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime) {
        return getDepartmentElectricityVOPageResult(pageNum, pageSize, deptIds, startTime, endTime, range, categoryName);
    }

    @Operation(summary = "9.部门排名导出")
    @GetMapping("/export/department/rank")
    public void exportDepartmentRanking(
            @Parameter(description = "页码，默认为1")
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页数量，默认为10")
            @RequestParam(defaultValue = "10") Integer pageSize,

            @Parameter(description = "时间范围: today-今天/yesterday-昨天/week-本周/lastWeek-上周/month-本月/lastMonth-上月/custom-自定义")
            @RequestParam(defaultValue = "today") String range,

            @Parameter(description = "房间ID")
            @RequestParam(required = false) String roomIds,
            @Parameter(description = "分类名称")
            @RequestParam(required = false) String categoryName,

            @Parameter(description = "自定义开始时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "自定义结束时间(yyyy-MM-dd格式)")
            @RequestParam(required = false) String endTime,
            HttpServletResponse response
    ) throws IOException {
        String fileName = "部门排名.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        PageResult<DepartmentElectricityVO> departmentElectricityVOPageResult = getDepartmentElectricityVOPageResult(pageNum, pageSize, roomIds, startTime, endTime, range, categoryName);
        List<DepartmentElectricityVO> list = departmentElectricityVOPageResult.getData().getList();
        List<ExportDepartmentRank> exportDepartments = new ArrayList<>();
        for (DepartmentElectricityVO departmentElectricityVO : list) {
            ExportDepartmentRank exportDepartmentRank = new ExportDepartmentRank();
            exportDepartmentRank.setDepartmentName(departmentElectricityVO.getDepartmentName());
            exportDepartmentRank.setTotalElectricity(departmentElectricityVO.getTotalElectricity());
            exportDepartmentRank.setCategoryName(departmentElectricityVO.getCategoryName());
            exportDepartments.add(exportDepartmentRank);
        }
        EasyExcel.write(response.getOutputStream(), ExportDepartmentRank.class).sheet("部门排名")
                .doWrite(exportDepartments);

    }

    @NotNull
    private PageResult<DepartmentElectricityVO> getDepartmentElectricityVOPageResult(Integer pageNum, Integer pageSize, String deptIds, String startTime, String endTime, String range, String categoryName) {
        try {
            // 获取所有部门
            List<Dept> allDepts = deptService.list();
            if (ObjectUtils.isEmpty(allDepts)) {
                Page<DepartmentElectricityVO> page = new Page<>(pageNum, pageSize);
                page.setTotal(0);
                page.setRecords(new ArrayList<>());
                return PageResult.success(page);
            }

            // 解析部门ID列表
            List<Long> deptIdList;
            if (StringUtils.isNotBlank(deptIds)) {
                deptIdList = Arrays.stream(deptIds.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .map(Long::parseLong)
                        .toList();
            } else {
                deptIdList = allDepts.stream().map(Dept::getId).toList();
            }

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
            Long categoryId = null;
            if (StringUtils.isNotEmpty(categoryName)) {
                //根据名称查询categoryId
                Category category = categoryService.getCategoryByName(categoryName);
                if (ObjectUtils.isNotEmpty(category)) {
                    categoryId = category.getId();
                }
            }
            // 查询所有房间的用电量数据
            List<RoomsElectricityVO> roomsElectricityList = electricityCalculationService.getRoomsElectricity(startTime, endTime, roomIdsStr, range, categoryId);

            // 构建部门用电量结果
            List<DepartmentElectricityVO> result = new ArrayList<>();

            // 按部门分组计算用电量
            for (Map.Entry<Long, List<Room>> entry : roomsByDept.entrySet()) {
                Long departmentId = entry.getKey();
                List<Room> roomsInDept = entry.getValue();

                // 获取部门信息
                Dept department = allDepts.stream()
                        .filter(dept -> dept.getId().equals(departmentId))
                        .findFirst()
                        .orElse(null);

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
                deptElectricityVO.setCategoryName(categoryName);
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
            log.error("分页查询各部门用电量失败 - deptIds: {}, range: {}", deptIds, range, e);
            return PageResult.success(null);
        }
    }
}