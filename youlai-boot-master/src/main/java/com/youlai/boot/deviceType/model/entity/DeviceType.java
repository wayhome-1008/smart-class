package com.youlai.boot.deviceType.model.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

/**
 * 设备类型字典(自维护)实体对象
 *
 * @author way
 * @since 2025-05-19 10:59
 */
@Getter
@Setter
@TableName("device_type")
public class DeviceType extends BaseEntity {

    /**
     * 设备类型名称
     */
    private String deviceType;
    /**
     * 创建人ID
     */
    private Long createBy;
    /**
     * 更新人ID
     */
    private Long updateBy;
}
