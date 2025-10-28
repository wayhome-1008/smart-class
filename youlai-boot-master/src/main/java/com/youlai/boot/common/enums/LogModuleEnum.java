package com.youlai.boot.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 日志模块枚举
 *
 * @author Ray
 * @since 2.10.0
 */
@Schema(enumAsRef = true)
@Getter
public enum LogModuleEnum {

    EXCEPTION("异常"),
    LOGIN("登录"),
    USER("用户"),
    DEPT("部门"),
    ROLE("角色"),
    MENU("菜单"),
    DICT("字典"),
    SETTING("系统配置"),
    OTHER("其他"),
    BUILDING("楼宇"),
    DEVICE("设备"),
    FLOOR("楼层"),
    ROOM("房间"),
    OPERATION("操作"),
    ALERT_EVENT("告警事件"),
    CATEGORY("设备分类"),
    Export("导出数据"),
    Job("定时任务"),
    SCENE("场景"),
    ALERT_RULE("告警配置"),
    WARNING("告警");

    @JsonValue
    private final String moduleName;

    LogModuleEnum(String moduleName) {
        this.moduleName = moduleName;
    }
}