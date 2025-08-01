package com.youlai.boot.scene.model.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

/**
 * 场景脚本实体对象
 *
 * @author way
 * @since 2025-07-29 11:58
 */
@Getter
@Setter
@TableName("scene_script")
public class SceneScript extends BaseEntity {

    /**
     * 场景ID
     */
    private Long sceneId;

    /** 场景脚本ID */
    private String scriptId;
    /**
     * 任务ID
     */
    private Long jobId;
    /**
     * 触发源（1=设备触发，2=定时触发，3=产品触发，4=执行告警）
     */
    private Integer source;
    /**
     * 脚本用途(1=数据流，2=触发器，3=执行动作)
     */
    private Integer scriptPurpose;
    /**
     * 操作符
     */
    private String operator;

    /** 物模型标识符 */
    private String modeId;

    /** 物模型名称 */
    private String modeName;

    /** 物模型值 */
    private String modeValue;
    /**
     * 功能类别（1=属性，2=功能，3=事件，4=设备升级，5=设备上线，6=设备下线）
     */
    private Integer type;
    /**
     * 设备数量
     */
    private Integer deviceCount;

    /** 设备编号，仅用于传递*/
    private String[] deviceNums;
    /**
     * cron表达式
     */
    private String cron;
    /**
     * 是否自定义表达式，1：是，0：否
     */
    private Integer isAdvance;
    /**
     * 数组索引
     */
    private String arrayIndex;
    /**
     * 数组索引名称
     */
    private String arrayIndexName;
    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 分类名称
     */
    private String categoryName;
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
}
