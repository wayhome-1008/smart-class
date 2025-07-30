package com.youlai.boot.scene.model.entity;

import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

/**
 * 场景设备实体对象
 *
 * @author way
 * @since 2025-07-29 12:03
 */
@Getter
@Setter
@TableName("scene_device")
public class SceneDevice extends BaseEntity {

    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 分类名称
     */
    private String categoryName;
    /**
     * 场景脚本ID
     */
    private String scriptId;
    /**
     * 场景ID
     */
    private Long sceneId;
    /**
     * 类型（2=触发器，3=执行动作）
     */
    private Integer type;
    /**
     * 触发源（1=设备触发，3=产品触发）
     */
    private Integer source;

    /** 设备编号（产品触发的没有设备编号） */
    private String serialNumber;
}
