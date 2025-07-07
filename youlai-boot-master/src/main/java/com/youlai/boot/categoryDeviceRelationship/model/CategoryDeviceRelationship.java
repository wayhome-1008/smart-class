package com.youlai.boot.categoryDeviceRelationship.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;

/**
 * 
 * @TableName category_device_relationship
 */
@TableName(value ="category_device_relationship")
@Data
public class CategoryDeviceRelationship extends BaseEntity {

    /**
     * 
     */
    private Long deviceId;

    /**
     * 
     */
    private Long categoryId;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;
}