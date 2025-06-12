package com.youlai.boot.floor.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.youlai.boot.device.model.vo.DeviceInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 楼层管理视图对象
 *
 * @author way
 * @since 2025-05-08 12:23
 */
@Getter
@Setter
@Schema( description = "楼层管理视图对象")
public class FloorVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "所属教学楼 ID（外键）")
    private Long buildingId;
    @Schema(description = "楼层号（如 1、2、-1（负一层））")
    private String floorNumber;
    private String buildingName;
    @Schema(description = "备注")
    private String remark;
    private LocalDateTime createTime;
    private Long createBy;
    private LocalDateTime updateTime;
    private Long updateBy;
    private Integer isDeleted;
    private List<DeviceInfoVO> deviceInfo;
    private Double temperature;
    private Double humidity;
    private Double Illuminance;
    private Boolean light;
    private Boolean plug;
    private Boolean human;

    @Schema(description = "开关状态")
    private Boolean isOpen;

}
