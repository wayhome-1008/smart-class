package com.youlai.boot.room.model.vo;

import com.youlai.boot.device.model.vo.DeviceInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 房间管理视图对象
 *
 * @author way
 * @since 2025-05-09 12:09
 */
@Getter
@Setter
@Schema(description = "房间管理视图对象")
public class RoomVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private Long id;
    @Schema(description = "所属楼层")
    private Long floorId;
    @Schema(description = "所属教学楼")
    private Long buildingId;
    @Schema(description = "所属教学楼名称")
    private String buildingName;
    @Schema(description = "楼层名称")
    private String floorName;
    @Schema(description = "房间号")
    private String classroomCode;
    @Schema(description = "所属部门")
    private String departmentName;
    @Schema(description = "备注")
    private String remark;
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
    @Schema(description = "房间本日用电量")
    private Double todayElectricity;

}
