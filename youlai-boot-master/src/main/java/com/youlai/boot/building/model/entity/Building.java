package com.youlai.boot.building.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 教学楼管理实体对象
 *
 * @author way
 * @since 2025-05-08 14:00
 */
@Getter
@Setter
@TableName("building")
public class Building extends BaseEntity {

    /**
     * 教学楼编号
     */
    private String buildingCode;
    /**
     * 教学楼名称
     */
    private String buildingName;
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
}
