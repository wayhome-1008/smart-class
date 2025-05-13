package com.youlai.boot.floor.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 楼层管理实体对象
 *
 * @author way
 * @since 2025-05-08 12:23
 */
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@TableName("floor")
public class Floor extends BaseEntity {

    /**
     * 所属教学楼 ID（外键）
     */
    private Long buildingId;
    /**
     * 楼层号（如 1、2、-1（负一层））
     */
    private String floorNumber;
    /**
     * 备注
     */
    private String remark;
    private Long createBy;
    private Long updateBy;
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
