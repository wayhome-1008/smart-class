package com.youlai.boot.room.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

/**
 * 房间管理实体对象
 *
 * @author way
 * @since 2025-05-09 12:09
 */
@Getter
@Setter
@TableName("room")
public class Room extends BaseEntity {

    /**
     * 所属楼层
     */
    private Long floorId;
    /**
     * 设备
     */
    private String deviceId;
    /**
     * 所属教学楼
     */
    private Long buildingId;
    /**
     * 所属部门
     */
    private Long departmentId;
    /**
     * 房间号
     */
    private String classroomCode;
    /**
     * 备注
     */
    private String remark;
    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;
    /**
     * 逻辑删除标识(0-未删除 1-已删除)
     */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
