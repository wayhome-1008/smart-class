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
    @Schema(description = "设备信息")
    private List<DeviceInfoVO> deviceInfo;
    @Schema(description = "房间温度")
    private Double temperature;
    @Schema(description = "房间湿度")
    private Double humidity;
    @Schema(description = "房间光照")
    private Double Illuminance;
    @Schema(description = "是否开灯")
    private Boolean light;
    @Schema(description = "是否开插座")
    private Boolean plug;
    @Schema(description = "是否有人")
    private Boolean human;
    @Schema(description = "总开关状态")
    private Boolean isOpen;
    @Schema(description = "开灯数量")
    private Integer lightNum;
    @Schema(description = "开插座数量")
    private Integer plugNum;

}
