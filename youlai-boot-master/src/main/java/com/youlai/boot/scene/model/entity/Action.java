package com.youlai.boot.scene.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

/**
 * 执行器实体对象
 *
 * @author way
 * @since 2025-07-30 17:28
 */
@Getter
@Setter
@TableName("scene_action")
public class Action extends BaseEntity {

    private Long sceneId;
    /**
     * 动作类型（DEVICE_EXECUTE/PRODUCT_EXECUTE/ALARM_EXECUTE）
     */
    private String type;
    /**
     * 设备ID列表
     */
    private String deviceIds;
    /**
     * 分类id
     */
    private Long categoryId;
    /**
     * 报警配置id
     */
    private Long alertRuleId;

    private String parameters;

    @TableField(exist = false)
    @JsonIgnore
    private Scene scene;
}
