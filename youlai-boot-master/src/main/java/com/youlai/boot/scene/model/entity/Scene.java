package com.youlai.boot.scene.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 场景交互实体对象
 *
 * @author way
 * @since 2025-07-29 11:51
 */
@Getter
@Setter
@TableName("scene")
public class Scene extends BaseEntity {

    /**
     * 场景名称
     */
    private String sceneName;

    /**
     * 任务id
     */
    private Long jobId;
    /**
     * 触发条件类型（ALL/ANY/NOT）
     */
    private String conditionType;
    /**
     * 静默周期(分钟)
     */
    private Integer silenceTime;
    /**
     * 执行方式（SERIAL/PARALLEL）
     */
    private String executeMode;
    /**
     * 延时执行(秒钟)
     */
    private Integer delaySeconds;
    /**
     * 场景状态（ 1启用 0禁用）
     */
    private Integer enable;

    /** 接收的触发器列表 */
    @TableField(exist = false)
    private List<Trigger> triggers;

    /** 接收的执行动作列表 */
    @TableField(exist = false)
    private List<Action> actions;
    /**
     * 创建者
     */
    private String createBy;
    /**
     * 更新者
     */
    private String updateBy;
    /**
     * 备注
     */
    private String remark;
}
