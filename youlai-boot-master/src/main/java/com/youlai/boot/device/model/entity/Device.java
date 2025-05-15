package com.youlai.boot.device.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

/**
 * 设备管理实体对象
 *
 * @author way
 * @since 2025-05-08 15:16
 */
@Getter
@Setter
@TableName("device")
public class Device extends BaseEntity {

    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 设备编号
     */
    private String deviceCode;
    /**
     * 教室
     */
    private Long deviceRoom;
    /**
     * 网关
     */
    private String deviceMac;
    /**
     * 设备类型
     */
    private Long deviceTypeItemId;
    @TableField(exist = false)
    private String deviceTypeItemName;
    /**
     * 通讯方式
     */
    private Long communicationModeItemId;
    @TableField(exist = false)
    private String communicationModeItemName;

    private String deviceNo;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 备注信息
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
    @TableLogic
    private Integer isDeleted;
}
