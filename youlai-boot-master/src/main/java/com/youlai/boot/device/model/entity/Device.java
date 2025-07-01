package com.youlai.boot.device.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.databind.JsonNode;
import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import com.youlai.boot.config.handler.JsonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

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
@TableName(value = "device", autoResultMap = true)// autoResultMap 启用自动结果映射
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
     * 分类物品id
     */
    private Long categoryId;


    @TableField(exist = false)
    private String roomName;
    /**
     * 网关
     */
    private String deviceMac;
    /**
     * 网关子设备绑定网关主键id
     */
    private Long deviceGatewayId;
    /**
     * 设备类型
     */
    private Long deviceTypeId;
    /**
     * 设备数据信息
     */
    // 指定数据库字段为JSON类型，并使用TypeHandler处理序列化
    @TableField(
            value = "device_info",
            typeHandler = JsonTypeHandler.class  // 自定义TypeHandler
    )
    private JsonNode deviceInfo;  // 存储JSON数据

    @TableField(exist = false)
    private String deviceType;
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
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    @TableField(exist = false)
    private Date deviceLastDate;
}
