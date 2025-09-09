package com.youlai.boot.room.listener;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.building.model.entity.Building;
import com.youlai.boot.building.service.BuildingService;
import com.youlai.boot.common.constant.SystemConstants;
import com.youlai.boot.common.result.ExcelResult;
import com.youlai.boot.floor.model.entity.Floor;
import com.youlai.boot.floor.service.FloorService;
import com.youlai.boot.room.model.dto.RoomImportDTO;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.service.RoomService;
import com.youlai.boot.system.model.entity.Dept;
import com.youlai.boot.system.service.DeptService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 *@Author: way
 *@CreateTime: 2025-09-09  14:42
 *@Description: TODO
 */
@Slf4j
public class RoomImportListener extends AnalysisEventListener<RoomImportDTO> {
    /**
     * Excel 导入结果
     */
    @Getter
    private final ExcelResult excelResult;
    private final BuildingService buildingService;
    private final FloorService floorService;
    private final RoomService roomService;
    private final DeptService deptService;

    /**
     * 当前行
     */
    private Integer currentRow = 1;

    /**
     * 构造方法
     */
    public RoomImportListener() {
        this.buildingService = SpringUtil.getBean(BuildingService.class);
        this.floorService = SpringUtil.getBean(FloorService.class);
        this.roomService = SpringUtil.getBean(RoomService.class);
        this.deptService = SpringUtil.getBean(DeptService.class);
        this.excelResult = new ExcelResult();
    }

    @Override
    public void invoke(RoomImportDTO roomImportDTO, AnalysisContext context) {
        log.info("解析到一条房间数据:{}", JSONUtil.toJsonStr(roomImportDTO));
        boolean validation = true;
        String errorMsg = "第" + currentRow + "行数据校验失败：";

        // 校验必填字段
        String buildingName = roomImportDTO.getBuildingName();
        String buildingCode = roomImportDTO.getBuildingCode();
        String floorNumber = roomImportDTO.getFloorNumber();
        String classRoomCode = roomImportDTO.getClassRoomCode();

        if (StrUtil.isBlank(buildingName)) {
            errorMsg += "楼宇名称为空；";
            validation = false;
        }

        if (StrUtil.isBlank(buildingCode)) {
            errorMsg += "楼宇编号为空；";
            validation = false;
        }

        if (StrUtil.isBlank(floorNumber)) {
            errorMsg += "楼层号为空；";
            validation = false;
        }

        if (StrUtil.isBlank(classRoomCode)) {
            errorMsg += "房间名称为空；";
            validation = false;
        }

        if (validation) {
            try {
                // 1. 检查楼宇是否已存在
                Building building = buildingService.getOne(new LambdaQueryWrapper<Building>()
                        .eq(Building::getBuildingCode, buildingCode));

                if (building == null) {
                    // 检查楼宇名称是否已存在
                    long buildingNameCount = buildingService.count(new LambdaQueryWrapper<Building>()
                            .eq(Building::getBuildingName, buildingName));
                    if (buildingNameCount == 0) {
                        // 楼宇不存在，创建新楼宇
                        building = new Building();
                        building.setBuildingName(buildingName);
                        building.setBuildingCode(buildingCode);
                        building.setIsDeleted(0);
                        building.setCreateBy(1L); // 默认创建人
                        building.setUpdateBy(1L); // 默认更新人
                        buildingService.save(building);
                    }
                } else {
                    // 楼宇已存在，检查名称是否匹配
                    if (!buildingName.equals(building.getBuildingName())) {
                        errorMsg += "楼宇编号已存在但名称不匹配；";
                        excelResult.setInvalidCount(excelResult.getInvalidCount() + 1);
                        excelResult.getMessageList().add(errorMsg);
                        currentRow++;
                        return;
                    }
                }

                // 2. 检查楼层是否已存在
                Floor floor = floorService.getOne(new LambdaQueryWrapper<Floor>()
                        .eq(Floor::getBuildingId, building.getId())
                        .eq(Floor::getFloorNumber, floorNumber));

                if (floor == null) {
                    // 楼层不存在，创建新楼层
                    floor = new Floor();
                    floor.setBuildingId(building.getId());
                    floor.setFloorNumber(floorNumber);
                    floor.setIsDeleted(0);
                    floorService.save(floor);
                }

                // 3. 检查房间是否已存在
                long roomCount = roomService.count(new LambdaQueryWrapper<Room>()
                        .eq(Room::getClassroomCode, classRoomCode));

                if (roomCount > 0) {
                    errorMsg += "房间已存在；";
                    excelResult.setInvalidCount(excelResult.getInvalidCount() + 1);
                    excelResult.getMessageList().add(errorMsg);
                    currentRow++;
                    return;
                }

                // 4. 处理房间
                Room room = new Room();
                room.setBuildingId(building.getId());
                room.setFloorId(floor.getId());
                room.setClassroomCode(classRoomCode);
                room.setIsDeleted(0);

                // 5. 处理部门（可选）
                String deptName = roomImportDTO.getDeptName();
                if (StrUtil.isNotBlank(deptName)) {
                    Dept dept = deptService.getOne(new LambdaQueryWrapper<Dept>()
                            .eq(Dept::getName, deptName));
                    if (dept != null) {
                        room.setDepartmentId(dept.getId());
                    } else {
                        // 部门不存在，可以记录警告但不阻止导入
                        log.warn("部门名称不存在: {}", deptName);
                    }
                }

                // 保存房间
                boolean saveResult = roomService.save(room);
                if (saveResult) {
                    excelResult.setValidCount(excelResult.getValidCount() + 1);
                } else {
                    excelResult.setInvalidCount(excelResult.getInvalidCount() + 1);
                    errorMsg += "第" + currentRow + "行数据保存失败；";
                    excelResult.getMessageList().add(errorMsg);
                }
            } catch (Exception e) {
                excelResult.setInvalidCount(excelResult.getInvalidCount() + 1);
                errorMsg += "第" + currentRow + "行数据保存异常：" + e.getMessage();
                excelResult.getMessageList().add(errorMsg);
                log.error("房间导入保存异常", e);
            }
        } else {
            excelResult.setInvalidCount(excelResult.getInvalidCount() + 1);
            excelResult.getMessageList().add(errorMsg);
        }
        currentRow++;
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("所有数据解析完成！");
    }
}
