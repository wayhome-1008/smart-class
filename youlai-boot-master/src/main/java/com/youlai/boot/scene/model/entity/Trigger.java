package com.youlai.boot.scene.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youlai.boot.scene.model.form.ThresholdCondition;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

import java.util.List;

/**
 * 触发器实体对象
 *
 * @author way
 * @since 2025-07-30 17:25
 */
@Getter
@Setter
@TableName("scene_trigger")
public class Trigger extends BaseEntity {

    private Long sceneId;
    /**
     * 触发类型（DEVICE_TRIGGER/PRODUCT_TRIGGER/TIMER_TRIGGER）
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
     * Cron表达式
     */
    private String cron;
    /**
     * 触发阈值条件（JSON数组，格式：[{"property":"温度","operator":">","value":30},...]）
     */
    private String threshold;
    /**
     * 阈值条件逻辑
     */
    private String thresholdLogic;

    @TableField(exist = false)
    @JsonIgnore
    private Scene scene;

    // 解析threshold为ThresholdCondition列表
    @JsonIgnore
    public List<ThresholdCondition> getThresholdConditions() {
        try {
            if (this.threshold == null || this.threshold.isEmpty()) {
                return List.of();
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(this.threshold, new TypeReference<List<ThresholdCondition>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("解析阈值条件失败", e);
        }
    }
}
