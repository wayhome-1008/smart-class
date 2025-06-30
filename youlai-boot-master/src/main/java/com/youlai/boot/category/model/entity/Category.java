package com.youlai.boot.category.model.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

/**
 * 分类管理实体对象
 *
 * @author way
 * @since 2025-06-30 18:52
 */
@Getter
@Setter
@TableName("category")
public class Category extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String categoryName;
    /**
     * icon
     */
    private String icon;
    /**
     * 是否禁用(0-启用 1-禁用)
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
    private Integer isDeleted;
}
